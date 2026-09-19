    package sv.vill.project_gps.menus
    import android.os.Bundle
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ArrayAdapter
    import androidx.fragment.app.Fragment
    import androidx.lifecycle.lifecycleScope
    import kotlinx.coroutines.launch
    import sv.vill.project_gps.data.AppDatabase
    import sv.vill.project_gps.data.DatabaseBuilder
    import sv.vill.project_gps.databinding.FragmentSelectorBinding

    class RutaSelectorFragment : Fragment() {

        private lateinit var binding: FragmentSelectorBinding
        private lateinit var db: AppDatabase

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentSelectorBinding.inflate(inflater, container, false)
            db = DatabaseBuilder.getInstance(requireContext())
            cargarSectores()
            return binding.root
        }

        private fun cargarSectores() {
            lifecycleScope.launch {
                val sectores = db.rutaDao().getAllSectores() // Ahora sí existe
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    sectores.map { "Sector ${it.numero}" }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
                binding.spinnerSectores.adapter = adapter
            }
        }
    }
