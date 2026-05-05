import { ref, onUnmounted } from 'vue'
import { readBarcodesFromImageData, getZXingModule, type ReaderOptions } from 'zxing-wasm/reader'

export function useScanner() {
  const isScanning = ref(false)
  const videoRef = ref<HTMLVideoElement | null>(null)
  const canvasRef = ref<HTMLCanvasElement | null>(null)
  let stream: MediaStream | null = null
  let rafId: number | null = null
  let zxingReady: Promise<any> | null = null
  let onDetected: ((code: string) => void) | null = null

  async function ensureZXing() {
    if (!zxingReady) {
      zxingReady = getZXingModule()
    }
    return zxingReady
  }

  async function startScan(callback: (code: string) => void) {
    onDetected = callback
    if (isScanning.value) {
      stopScan()
    }

    try {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } }
      })
    } catch (e: any) {
      const msg = e.name === 'NotAllowedError' ? '请允许摄像头权限'
        : e.name === 'NotFoundError' ? '未找到摄像头'
        : '摄像头启动失败'
      throw new Error(msg)
    }

    if (!videoRef.value) {
      stopScan()
      throw new Error('视频元素未就绪')
    }

    videoRef.value.srcObject = stream
    await videoRef.value.play()

    if (!canvasRef.value) {
      canvasRef.value = document.createElement('canvas')
    }
    isScanning.value = true
    const canvas = canvasRef.value
    const ctx = canvas.getContext('2d')!
    let lastScanTime = 0
    const readerOptions: ReaderOptions = {
      formats: ['Code128', 'Code39', 'Code93', 'Codabar', 'EAN-13', 'EAN-8', 'UPC-A', 'UPC-E', 'ITF'],
      tryHarder: true,
      tryRotate: true,
      tryInvert: true,
      maxNumberOfSymbols: 1,
    }

    async function tick() {
      if (!isScanning.value || !videoRef.value) return

      if (videoRef.value.readyState >= 2) {
        canvas.width = videoRef.value.videoWidth
        canvas.height = videoRef.value.videoHeight
        ctx.drawImage(videoRef.value, 0, 0)

        const now = Date.now()
        if (now - lastScanTime > 300) {
          lastScanTime = now
          try {
            await ensureZXing()
            const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
            const results = await readBarcodesFromImageData(imageData, readerOptions)
            if (results.length > 0 && results[0].isValid && results[0].text) {
              stopScan()
              onDetected?.(results[0].text)
              return
            }
          } catch {}
        }
      }

      rafId = requestAnimationFrame(tick)
    }

    rafId = requestAnimationFrame(tick)
  }

  function stopScan() {
    isScanning.value = false
    if (rafId != null) { cancelAnimationFrame(rafId); rafId = null }
    if (stream) {
      stream.getTracks().forEach(t => t.stop())
      stream = null
    }
  }

  onUnmounted(stopScan)

  return { isScanning, videoRef, canvasRef, startScan, stopScan }
}
