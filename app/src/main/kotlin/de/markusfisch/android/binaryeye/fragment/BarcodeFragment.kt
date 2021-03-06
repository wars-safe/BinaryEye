package de.markusfisch.android.binaryeye.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.EditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.*
import de.markusfisch.android.binaryeye.view.doOnApplyWindowInsets
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.widget.ConfinedScalingImageView
import de.markusfisch.android.binaryeye.widget.toast
import de.markusfisch.android.binaryeye.zxing.Zxing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BarcodeFragment : Fragment() {
	private enum class FileType {
		PNG, SVG, TXT
	}

	private var barcodeBitmap: Bitmap? = null
	private var barcodeSvg: String? = null
	private var barcodeTxt: String? = null
	private var content: String = ""
	private var format: BarcodeFormat? = null

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setHasOptionsMenu(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		state: Bundle?
	): View? {
		val ac = activity ?: return null
		ac.setTitle(R.string.view_barcode)

		val view = inflater.inflate(
			R.layout.fragment_barcode,
			container,
			false
		)

		val args = arguments ?: return view
		val content = args.getString(CONTENT) ?: return view
		val format = args.getSerializable(FORMAT) as BarcodeFormat? ?: return view
		val ecl = args.getSerializable(
			ERROR_CORRECTION_LEVEL
		) as ErrorCorrectionLevel?
		val size = args.getInt(SIZE)
		try {
			barcodeBitmap = Zxing.encodeAsBitmap(content, format, size, size, ecl)
			barcodeSvg = Zxing.encodeAsSvg(content, format, size, size, ecl)
			barcodeTxt = Zxing.encodeAsTxt(content, format, ecl)
		} catch (e: Exception) {
			var message = e.message
			if (message == null || message.isEmpty()) {
				message = getString(R.string.error_encoding_barcode)
			}
			message?.let {
				ac.toast(message)
			}
			fragmentManager.popBackStack()
			return null
		}
		this.content = content
		this.format = format

		val imageView = view.findViewById<ConfinedScalingImageView>(
			R.id.barcode
		)
		imageView.setImageBitmap(barcodeBitmap)
		imageView.post {
			// make sure to invoke this after ScalingImageView.onLayout()
			imageView.minWidth /= 2f
		}

		view.findViewById<View>(R.id.share).setOnClickListener {
			pickFileType(context, R.string.share_as) {
				shareAs(it)
			}
		}

		(view.findViewById(R.id.inset_layout) as View).setPaddingFromWindowInsets()
		imageView.doOnApplyWindowInsets { v, insets ->
			(v as ConfinedScalingImageView).insets.set(insets)
		}

		return view
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.fragment_barcode, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.copy_to_clipboard -> {
				barcodeTxt?.let {
					context.copyToClipboard(it)
					context.toast(R.string.copied_to_clipboard)
				}
				true
			}
			R.id.export_to_file -> {
				pickFileType(context, R.string.export_as) {
					askForFileNameAndSave(it)
				}
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	private fun pickFileType(
		context: Context,
		title: Int,
		action: (FileType) -> Unit
	) {
		val fileTypes = FileType.values()
		AlertDialog.Builder(context)
			.setTitle(title)
			.setItems(fileTypes.map { it.name }.toTypedArray()) { _, which ->
				action(fileTypes[which])
			}
			.show()
	}

	// dialogs do not have a parent view
	@SuppressLint("InflateParams")
	private fun askForFileNameAndSave(fileType: FileType) {
		val ac = activity ?: return
		if (!hasWritePermission(ac)) {
			return
		}
		val view = ac.layoutInflater.inflate(R.layout.dialog_save_file, null)
		val editText = view.findViewById<EditText>(R.id.file_name)
		editText.setText(encodeFileName("${format.toString()}_$content"))
		AlertDialog.Builder(ac)
			.setView(view)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val fileName = editText.text.toString()
				when (fileType) {
					FileType.PNG -> saveAs(
						addSuffixIfNotGiven(fileName, ".png"),
						MIME_PNG
					) {
						barcodeBitmap?.saveAsPng(it)
					}
					FileType.SVG -> saveAs(
						addSuffixIfNotGiven(fileName, ".svg"),
						MIME_SVG
					) { outputStream ->
						barcodeSvg?.let {
							outputStream.write(it.toByteArray())
						}
					}
					FileType.TXT -> saveAs(
						addSuffixIfNotGiven(fileName, ".txt"),
						MIME_TXT
					) { outputStream ->
						barcodeTxt?.let {
							outputStream.write(it.toByteArray())
						}
					}
				}
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
			}
			.show()
	}

	private fun saveAs(
		fileName: String,
		mimeType: String,
		write: (outputStream: OutputStream) -> Unit
	) {
		val ac = activity ?: return
		GlobalScope.launch {
			val message = writeExternalFile(ac, fileName, mimeType, write).toSaveResult()
			GlobalScope.launch(Main) {
				ac.toast(message)
			}
		}
	}

	private fun shareAs(fileType: FileType) {
		when (fileType) {
			FileType.PNG -> barcodeBitmap?.let { share(it) }
			FileType.SVG -> barcodeSvg?.let { shareText(context, it, MIME_SVG) }
			FileType.TXT -> barcodeTxt?.let { shareText(context, it) }
		}
	}

	private fun share(bitmap: Bitmap) {
		GlobalScope.launch(Dispatchers.IO) {
			val file = File(
				context.externalCacheDir,
				"shared_barcode.png"
			)
			val success = try {
				FileOutputStream(file).use {
					bitmap.saveAsPng(it)
				}
				true
			} catch (e: IOException) {
				false
			}
			GlobalScope.launch(Main) {
				if (success) {
					shareFile(context, file, "image/png")
				} else {
					activity?.toast(R.string.error_saving_file)
				}
			}
		}
	}

	companion object {
		private const val CONTENT = "content"
		private const val FORMAT = "format"
		private const val ERROR_CORRECTION_LEVEL = "error_correction_level"
		private const val SIZE = "size"
		private const val MIME_PNG = "image/png"
		private const val MIME_SVG = "image/svg+xmg"
		private const val MIME_TXT = "text/plain"

		fun newInstance(
			content: String,
			format: BarcodeFormat,
			size: Int,
			errorCorrectionLevel: ErrorCorrectionLevel? = null
		): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			args.putSerializable(FORMAT, format)
			errorCorrectionLevel?.let {
				args.putSerializable(ERROR_CORRECTION_LEVEL, it)
			}
			args.putInt(SIZE, size)
			val fragment = BarcodeFragment()
			fragment.arguments = args
			return fragment
		}
	}
}

private fun Bitmap.saveAsPng(outputStream: OutputStream, quality: Int = 90) {
	this.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
}

private val fileNameCharacters = "[^A-Za-z0-9]".toRegex()
private fun encodeFileName(name: String): String =
	fileNameCharacters.replace(name, "_").take(16).trim('_').toLowerCase(Locale.getDefault())
