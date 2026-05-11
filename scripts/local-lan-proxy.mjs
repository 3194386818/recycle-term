import fs from "node:fs";
import http from "node:http";
import https from "node:https";
import net from "node:net";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const certRoot = process.env.RECYCLE_CERT_ROOT || "/mnt/c/Users/xiaohei/Desktop/recycle-term-local-nginx";
const frontendTarget = new URL(process.env.RECYCLE_FRONTEND_URL || "http://127.0.0.1:5173");
const backendTarget = new URL(process.env.RECYCLE_BACKEND_URL || "http://127.0.0.1:8081");
const httpPort = Number(process.env.RECYCLE_HTTP_PORT || 8080);
const httpsPort = Number(process.env.RECYCLE_HTTPS_PORT || 8443);

const tlsOptions = {
  cert: fs.readFileSync(path.join(certRoot, "certs", "recycle.local.pem")),
  key: fs.readFileSync(path.join(certRoot, "certs", "recycle.local-key.pem")),
};

const proxyRequest = (target, req, res) => {
  const upstreamUrl = new URL(req.url || "/", target);
  const headers = { ...req.headers, host: target.host };
  const options = {
    protocol: target.protocol,
    hostname: target.hostname,
    port: target.port,
    method: req.method,
    path: `${upstreamUrl.pathname}${upstreamUrl.search}`,
    headers,
  };

  const upstream = http.request(options, (upstreamRes) => {
    res.writeHead(upstreamRes.statusCode || 502, upstreamRes.headers);
    upstreamRes.pipe(res);
  });

  upstream.on("error", (error) => {
    res.writeHead(502, { "content-type": "text/plain; charset=utf-8" });
    res.end(`代理失败: ${error.message}`);
  });

  req.pipe(upstream);
};

const serveCert = (filename, res) => {
  const certPath = path.join(certRoot, "www", filename);
  if (!fs.existsSync(certPath)) {
    res.writeHead(404, { "content-type": "text/plain; charset=utf-8" });
    res.end("证书文件不存在");
    return;
  }
  res.writeHead(200, {
    "content-type": filename.endsWith(".crt") ? "application/x-x509-ca-cert" : "application/x-pem-file",
    "content-disposition": `attachment; filename=\"recycle-${filename}\"`,
  });
  fs.createReadStream(certPath).pipe(res);
};

const handleHttps = (req, res) => {
  const url = new URL(req.url || "/", "https://local.invalid");
  if (url.pathname === "/cert/rootCA.crt") return serveCert("rootCA.crt", res);
  if (url.pathname === "/cert/rootCA.pem") return serveCert("rootCA.pem", res);
  if (url.pathname.startsWith("/api")) return proxyRequest(backendTarget, req, res);
  return proxyRequest(frontendTarget, req, res);
};

const upgradeServer = http.createServer();
upgradeServer.on("upgrade", (req, socket, head) => {
  const upstream = net.connect(Number(frontendTarget.port || 80), frontendTarget.hostname, () => {
    upstream.write(`${req.method} ${req.url} HTTP/${req.httpVersion}\r\n`);
    for (const [name, value] of Object.entries(req.headers)) {
      upstream.write(`${name}: ${Array.isArray(value) ? value.join(", ") : value}\r\n`);
    }
    upstream.write("\r\n");
    if (head.length) upstream.write(head);
    socket.pipe(upstream).pipe(socket);
  });
  upstream.on("error", () => socket.destroy());
});

const redirectServer = http.createServer((req, res) => {
  if (req.url === "/cert/rootCA.crt") return serveCert("rootCA.crt", res);
  if (req.url === "/cert/rootCA.pem") return serveCert("rootCA.pem", res);
  const host = (req.headers.host || "192.168.1.2").replace(/:.*/, "");
  res.writeHead(301, { location: `https://${host}${req.url || "/"}` });
  res.end();
});

const httpsServer = https.createServer(tlsOptions, handleHttps);
httpsServer.on("upgrade", upgradeServer.emit.bind(upgradeServer, "upgrade"));

redirectServer.listen(httpPort, "0.0.0.0", () => {
  console.log(`HTTP redirect/cert server listening on ${httpPort}`);
});
httpsServer.listen(httpsPort, "0.0.0.0", () => {
  console.log(`HTTPS proxy listening on ${httpsPort}`);
  console.log(`Frontend -> ${frontendTarget.href}`);
  console.log(`Backend  -> ${backendTarget.href}`);
});
