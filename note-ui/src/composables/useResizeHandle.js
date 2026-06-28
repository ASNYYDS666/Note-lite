import { ref } from 'vue'

export function useResizeHandle(options = {}) {
  const { min = 180, max = 600, reverse = false, getWidth, onResize } = options
  const dragging = ref(false)

  function onMouseDown(e) {
    e.preventDefault()
    dragging.value = true
    const startX = e.clientX
    const startWidth = getWidth()

    document.body.style.userSelect = 'none'
    document.body.style.cursor = 'col-resize'

    function onMouseMove(e) {
      if (!dragging.value) return
      const raw = e.clientX - startX
      const delta = reverse ? -raw : raw
      const newWidth = Math.min(max, Math.max(min, startWidth + delta))
      onResize(newWidth)
    }

    function onMouseUp() {
      dragging.value = false
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
      document.removeEventListener('mousemove', onMouseMove)
      document.removeEventListener('mouseup', onMouseUp)
    }

    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', onMouseUp)
  }

  return { dragging, onMouseDown }
}
