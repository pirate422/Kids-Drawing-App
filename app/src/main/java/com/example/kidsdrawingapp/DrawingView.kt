package com.example.kidsdrawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View



class DrawingView(context:Context,attrs:AttributeSet): View(context,attrs) {

    private var mDrawPath : CustomPath? =null
    private var mCanvasBitmap:Bitmap? =null
    private var mDrawPaint: Paint? =null
    private var mCanvasPaint: Paint? =null
    private var mBrushSize:Float =0.toFloat()
    private var color= Color.BLACK
    private var canvas:Canvas? = null
    // for lines not to be disappear
    private val mPaths=ArrayList<CustomPath>()

    // for undo button
    private val mUndoPaths=ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }
      fun onClickUndo(){
        if(mPaths.size>0){
            mUndoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
        }
    }

   private fun setUpDrawing(){
        mDrawPaint=Paint()
        mDrawPath=CustomPath(color,mBrushSize)
        mDrawPaint!!.color=color
        mDrawPath!!.brushthickness=20.toFloat()
        mCanvasPaint=Paint(Paint.DITHER_FLAG)

       mDrawPaint?.style = Paint.Style.STROKE // This is to draw a STROKE style
       mDrawPaint?.strokeJoin = Paint.Join.ROUND // This is for store join
       mDrawPaint?.strokeCap = Paint.Cap.ROUND // This is for stroke Cap
   }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap= Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas= Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!,0f,0f,mCanvasPaint)
        // for lines not to be disappear
        for(path in mPaths){
            mDrawPaint!!.color=path.color
            mDrawPaint!!.strokeWidth=path.brushthickness
            canvas.drawPath(path,mDrawPaint!!)
        }

        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.color=mDrawPath!!.color
            mDrawPaint!!.strokeWidth=mDrawPath!!.brushthickness
            canvas.drawPath(mDrawPath!!,mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX=event?.x
        val touchY=event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN->{
                mDrawPath!!.color=color
                mDrawPath!!.brushthickness=mBrushSize

                mDrawPath!!.reset()

                mDrawPath!!.moveTo(touchX!!,touchY!!)
            }

            MotionEvent.ACTION_MOVE->{
                mDrawPath!!.lineTo(touchX!!,touchY!!)
            }

            MotionEvent.ACTION_UP->{
                // store the drawPath i.e. wherever u have drawn, in paths
                mPaths.add(mDrawPath!!)
                mDrawPath=CustomPath(color,mBrushSize)
            }
            else -> return false
        }

        invalidate()

        return true
        //return super.onTouchEvent(event)
    }


    fun setSizeForBrush(newSize:Float){
        mBrushSize=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                             newSize,resources.displayMetrics)
        mDrawPaint!!.strokeWidth=mBrushSize
    }


    fun setColor(newColor:String){
        color=Color.parseColor(newColor)
        mDrawPaint!!.color=color
    }


    internal inner class CustomPath(var color :Int,
                                    var brushthickness:Float): Path() {

    }




}


