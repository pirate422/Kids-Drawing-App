package com.example.kidsdrawingapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.Canvas
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var drawingView:DrawingView?=null
    private var mImageButtonCurrentPaint:ImageButton?=null
      var  customProgressDialog: Dialog? = null

    //for external storage
    val openGalleryLauncher:ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode== RESULT_OK && result.data!=null){
            val imageBackground:ImageView=findViewById(R.id.iv_background)

            imageBackground.setImageURI(result.data?.data)


        }
    }

    // for permissions
    @SuppressLint("SuspiciousIndentation")
    val requestPermission:ActivityResultLauncher<Array<String>> =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions->
                permissions.entries.forEach{
                    val permissionName=it.key
                    val isGranted=it.value

                    if(isGranted){
                        Toast.makeText(this,"Permission is granted",Toast.LENGTH_LONG).show()

                    val pickIntent=Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)


                    }else{
                        // u need to use right manifest so import in manually
                        if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                            Toast.makeText(this,"oops Permission is denied",Toast.LENGTH_LONG).show()
                        }
                    }

                }
            }




    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView=findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        //To select colors
        var linearLayoutPaintColor=findViewById<LinearLayout>(R.id.ll_paint_color)
        mImageButtonCurrentPaint=linearLayoutPaintColor[0] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )


       val ibBrush:ImageButton= findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
      }

        val ibUndo:ImageButton= findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView!!.onClickUndo()
        }

        val ibGallery:ImageButton=findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }

        val ibSave:ImageButton = findViewById(R.id.ib_save)
        //set onclick listener
        // TODO(Step 3 : Adding an click event to save or exporting the image to your phone storage.)
        ibSave.setOnClickListener{
            //check if permission is allowed
            if (isReadStorageAllowed()){
                showProgressDialog()
                //launch a coroutine block
                lifecycleScope.launch{
                    //reference the frame layout
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    //Save the image to the device
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }
    }





    private fun showBrushSizeChooserDialog() {

        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size :")


        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }


        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }


        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view:View){

        if(view!==mImageButtonCurrentPaint){

            val imageButton= view as ImageButton
            val colorTag=imageButton.tag.toString()
            drawingView!!.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )


            mImageButtonCurrentPaint=view

        }
    }


    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether <em>you</em> have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }



    private fun requestStoragePermission(){

        if(ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        {
            showRationalDialog("Kids Drawing App","Kids Drawing App" +
                    "needs to Access Your External Storage")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE ,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

            ))
        }
    }

   private fun showRationalDialog(
       title:String,
       message: String
   ){
       val builder:AlertDialog.Builder = AlertDialog.Builder(this)
       builder.setTitle(title)
           .setMessage(message)
           .setPositiveButton("Cancel"){ dialog,_->

               dialog.dismiss()

           }
       builder.create().show()
   }


    private fun getBitmapFromView(view: View): Bitmap {

        // view cannot be save so covert view into bitmap to store
        val returnedBitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(returnedBitmap)

        //Get the view's background

        val bgDrawable = view.background
        if (bgDrawable != null) {

            //has background drawable, then draw it on the canvas

            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        // draw the view on the canvas
        view.draw(canvas)


        return returnedBitmap
    }



    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream() // Creates a new byte array output stream.
                    // The buffer capacity is initially 32 bytes, though its size increases if necessary.
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    /**
                     * Write a compressed version of the bitmap to the specified outputstream.
                     * If this returns true, the bitmap can be reconstructed by passing a
                     * corresponding inputstream to BitmapFactory.decodeStream(). Note: not
                     * all Formats support all bitmap configs directly, so it is possible that
                     * the returned bitmap from BitmapFactory could be in a different bitdepth,
                     * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
                     * pixels).
                     *
                     * @param format   The format of the compressed image
                     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
                     *                 small size, 100 meaning compress for max quality. Some
                     *                 formats, like PNG which is lossless, will ignore the
                     *                 quality setting
                     * @param stream   The outputstream to write the compressed data.
                     * @return true if successfully compressed to the specified stream.
                     */
                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.
                    val fo =
                        FileOutputStream(f) // Creates a file output stream to write to the file represented by the specified object.
                    fo.write(bytes.toByteArray()) // Writes bytes from the specified byte array to this file output stream.
                    fo.close() // Closes this file output stream and releases any system resources associated with this stream. This file output stream may no longer be used for writing bytes.
                    result = f.absolutePath // The file absolute path is return as a result.
                    //We switch from io to ui thread to show a toast
                    runOnUiThread {

                        cancelProgressDialog()

                        if (!result.isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()

                            shareImage(result)

                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }


    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }



    private fun shareImage(result:String){
        // TODO (Step 1 - Sharing the downloaded Image file)
        // START

        /*MediaScannerConnection provides a way for applications to pass a
        newly created or downloaded media file to the media scanner service.
        The media scanner service will read metadata from the file and add
        the file to the media content provider.
        The MediaScannerConnectionClient provides an interface for the
        media scanner service to return the Uri for a newly scanned file
        to the client of the MediaScannerConnection class.*/
        /*scanFile is used to scan the file when the connection is established with MediaScanner.*/
        MediaScannerConnection.scanFile(
            this@MainActivity, arrayOf(result), null
        ) { path, uri ->
            // This is used for sharing the image after it has being stored in the storage.
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                uri
            ) // A content: URI holding a stream of data associated with the Intent, used to supply the data being sent.
            shareIntent.type =
                "image/png" // The MIME type of the data being handled by this intent.
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share"
                )
            )// Activity Action: Display an activity chooser,
            // allowing the user to pick what they want to before proceeding.
            // This can be used as an alternative to the standard activity picker
            // that is displayed by the system when you try to start an activity with multiple possible matches,
            // with these differences in behavior:
        }
        // END
    }


}