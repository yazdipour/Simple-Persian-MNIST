package io.github.yazdipour.mnist_kotlin

import android.graphics.PointF
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import io.github.yazdipour.mnist_kotlin.models.Classifier
import io.github.yazdipour.mnist_kotlin.models.TensorFlowClassifier
import io.github.yazdipour.mnist_kotlin.views.DrawModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.log


class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnTouchListener {
    private val PIXEL_WIDTH = 28


    // views
    private val mClassifiers = ArrayList<Classifier>()
    private var drawModel: DrawModel? = null
    private val mTmpPiont = PointF()

    private var mLastX: Float = 0.toFloat()
    private var mLastY: Float = 0.toFloat()
    //the activity lifecycle

    override//OnResume() is called when the user resumes his Activity which he left a while ago,
    fun onResume() {
        drawView?.onResume()
        super.onResume()
    }

    override//OnPause() is called when the user receives an event like a call or a text message,
    fun onPause() {
        drawView?.onPause()
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawModel = DrawModel(PIXEL_WIDTH, PIXEL_WIDTH)
        drawView?.setModel(drawModel)
        drawView?.setOnTouchListener(this)
        btn_clear?.setOnClickListener(this)
        btn_class?.setOnClickListener(this)

        // tensorflow
        //load up our saved model to perform inference from local storage
        loadModel()
    }
    //draw line down

    private fun processTouchDown(event: MotionEvent) {
        //calculate the x, y coordinates where the user has touched
        mLastX = event.x
        mLastY = event.y
        //user them to calcualte the position
        drawView?.calcPos(mLastX, mLastY, mTmpPiont)
        //store them in memory to draw a line between the
        //difference in positions
        val lastConvX = mTmpPiont.x
        val lastConvY = mTmpPiont.y
        //and begin the line drawing
        drawModel?.startLine(lastConvX, lastConvY)
    }

    //the main drawing function
    //it actually stores all the drawing positions
    //into the drawmodel object
    //we actually render the drawing from that object
    //in the drawrenderer class
    private fun processTouchMove(event: MotionEvent) {
        val x = event.x
        val y = event.y

        drawView?.calcPos(x, y, mTmpPiont)
        val newConvX = mTmpPiont.x
        val newConvY = mTmpPiont.y
        drawModel?.addLineElem(newConvX, newConvY)

        mLastX = x
        mLastY = y
        drawView?.invalidate()
    }

    private fun processTouchUp() {
        drawModel?.endLine()
    }

    //creates a model object in memory using the saved tensorflow protobuf model file
    //which contains all the learned weights
    private fun loadModel() {
        Thread(Runnable {
            try {
                //add our classifiers to our classifier arraylist
                mClassifiers.add(
                        TensorFlowClassifier.create(assets, "TensorFlow",
                                "opt_mnist_convnet-tf.pb", "labels.txt", PIXEL_WIDTH,
                                "input", "output", true))
            } catch (e: Exception) {
                throw RuntimeException("Error initializing classifiers!", e)
            }
        }).start()
    }

    override fun onClick(view: View) {
        if (view.id == R.id.btn_clear) {
            drawModel?.clear()
            drawView?.reset()
            drawView?.invalidate()
            tfRes?.setText("")
        } else 
        if (view.id == R.id.btn_class) {
            val pixels = drawView?.getPixelData()
            var text = ""
            //for each classifier in our array
            for (classifier in mClassifiers) {
                //perform classification on the image
                val res = classifier.recognize(pixels)
                //if it can't classify, output a question mark
                if (res.getLabel() == null) {
                    text += classifier.name() + ": ?\n"
                } else {
                    //else output its name
                    text += String.format("%s: %s, %f\n", classifier.name(), res.getLabel(),
                            res.getConf())
                }
            }
            tfRes?.setText(text)
        }
    }

    override//this method detects which direction a user is moving
    //their finger and draws a line accordingly in that direction
    fun onTouch(v: View, event: MotionEvent): Boolean {
        //get the action and store it as an int
        val action = event.action and MotionEvent.ACTION_MASK
        //actions have predefined ints, lets match
        //to detect, if the user has touched, which direction the users finger is
        //moving, and if they've stopped moving

        //if touched
        if (action == MotionEvent.ACTION_DOWN) {
            //begin drawing line
            processTouchDown(event)
            return true
            //draw line in every direction the user moves
        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event)
            return true
            //if finger is lifted, stop drawing
        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp()
            return true
        }
        return false
    }
}
