package doodle

import doodle.core._

import org.scalajs.dom

package object js {
  def draw(image: Image, id: String): Unit =
    draw(image, canvasElement(id))

  def draw(img: Image, canvas: dom.HTMLCanvasElement): Unit = {
    val ctx    = canvasContext(canvas)
    val origin = canvasCenter(canvas)
    draw(img, origin, DrawingContext.blackLines, ctx)
  }

  def animate(anim: Animation, id: String): Unit =
    animate(anim, canvasElement(id))

  def animate(anim: Animation, canvas: dom.HTMLCanvasElement): Unit = {
    def nextFrame(): Unit =
      dom.requestAnimationFrame { (_: Double) =>
        animate(anim.animate, canvas)
      }

    draw(anim.draw, canvas)
    dom.setTimeout(nextFrame _, 1000/24)
  }

  private def draw(img: Image, origin: Point, context: DrawingContext, ctx: dom.CanvasRenderingContext2D): Unit = {
    def doStrokeAndFill() = {
      context.fill.foreach {
        case Fill(color) => {
          ctx.fillStyle = color.toCanvas
          ctx.fill()
        }
      }

      context.stroke.foreach {
        case Stroke(width, color, cap, join) => {
          ctx.lineWidth = width
          ctx.lineCap = cap.toCanvas
          ctx.lineJoin = join.toCanvas
          ctx.strokeStyle = color.toCanvas
          ctx.stroke()
        }
      }
    }

    img match {
      case Circle(r) =>
        ctx.beginPath()
        ctx.arc(origin.x, origin.y, r, 0.0, Math.PI * 2)
        ctx.closePath()
        doStrokeAndFill()

      case Rectangle(w, h) =>
        ctx.beginPath()
        ctx.rect(origin.x - w/2, origin.y - h/2, w, h)
        ctx.closePath()
        doStrokeAndFill()

      case Triangle(w, h) =>
        ctx.beginPath()
        ctx.moveTo(origin.x      , origin.y - h/2)
        ctx.lineTo(origin.x + w/2, origin.y + h/2)
        ctx.lineTo(origin.x - w/2, origin.y + h/2)
        ctx.closePath()
        doStrokeAndFill()

      case Overlay(t, b) =>
        draw(b, origin, context, ctx)
        draw(t, origin, context, ctx)

      case b @ Beside(l, r) =>
        val box = BoundingBox(b)
        val lBox = BoundingBox(l)
        val rBox = BoundingBox(r)

        val lOriginX = origin.x + box.left + (lBox.width / 2)
        val rOriginX = origin.x + box.right - (rBox.width / 2)
        // Beside always vertically centers l and r, so we don't need
        // to calculate center ys for l and r.

        draw(l, Point(lOriginX, origin.y), context, ctx)
        draw(r, Point(rOriginX, origin.y), context, ctx)
      case a @ Above(t, b) =>
        val box = BoundingBox(a)
        val tBox = BoundingBox(t)
        val bBox = BoundingBox(b)

        val tOriginY = origin.y + box.top + (tBox.height / 2)
        val bOriginY = origin.y + box.bottom - (bBox.height / 2)

        draw(t, Point(origin.x, tOriginY), context, ctx)
        draw(b, Point(origin.x, bOriginY), context, ctx)
      case At(vec, i) =>
        draw(i, origin + vec, context, ctx)

      case ContextTransform(f, i) =>
        draw(i, origin, f(context), ctx)

      case d: Drawable =>
        draw(d.draw, origin, context, ctx)
    }
  }

  private def canvasElement(id: String): dom.HTMLCanvasElement =
    dom.document.getElementById(id).asInstanceOf[dom.HTMLCanvasElement]

  private def canvasContext(canvas: dom.HTMLCanvasElement): dom.CanvasRenderingContext2D =
    canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private def canvasCenter(canvas: dom.HTMLCanvasElement): Point =
    Point(canvas.width / 2, canvas.height / 2)
}