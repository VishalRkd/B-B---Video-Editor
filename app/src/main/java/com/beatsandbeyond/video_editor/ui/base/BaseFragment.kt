package com.beatsandbeyond.video_editor.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.beatsandbeyond.video_editor.core.utils.Logger

/**
 * Abstract base class for all Fragments in B&B Video Editor.
 *
 * Provides:
 * - Automatic ViewBinding inflation and cleanup (prevents memory leaks)
 * - Consistent logging tag derived from the concrete class name
 * - Lifecycle logging for debugging during development
 *
 * All concrete fragments must implement [inflateBinding] to provide their binding type.
 *
 * Usage:
 * ```
 * class HomeFragment : BaseFragment<FragmentHomeBinding>() {
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         FragmentHomeBinding.inflate(inflater, container, false)
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *         // Setup UI here
 *     }
 * }
 * ```
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    protected val logTag: String get() = this::class.java.simpleName

    private var _binding: VB? = null

    /** Non-null access to the view binding. Only valid between [onCreateView] and [onDestroyView]. */
    protected val binding: VB
        get() = requireNotNull(_binding) {
            "ViewBinding accessed outside of valid lifecycle (onCreateView to onDestroyView)"
        }

    /**
     * Subclasses must implement this to provide an inflated binding instance.
     */
    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Null the binding to prevent memory leaks when the fragment's view is destroyed
        // but the fragment itself remains in the backstack.
        _binding = null
        Logger.d(logTag, "onDestroyView: binding released")
    }
}
