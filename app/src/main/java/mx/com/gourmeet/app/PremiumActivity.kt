package mx.com.gourmeet.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import mx.com.gourmeet.app.databinding.ActivityPremiumBinding
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBinding
    private lateinit var billingClient: BillingClient
    private val productosPremium = mutableMapOf<String, ProductDetails>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("GOURMEET_BILLING", "PremiumActivity inició")


        // ==========================================
        // BOTÓN REGRESAR
        // ==========================================

        binding.btnRegresar.setOnClickListener {
            finish()
        }


        // ==========================================
        // PLANES
        // ==========================================

        val planes = listOf(

            // ------------------------------------------
            // PLAN INDIVIDUAL
            // ------------------------------------------

            PlanPremium(
                imagen = R.drawable.ic_plan_individual,
                nombre = "Plan Individual",
                precioMensual = "\$50 / mes",
                precioAnual = "\$550 / año",

                // ID del producto de Google Play
                productoId = "gourmeet_individual",

                // Base plans de Google Play
                planBasicoMensual = "mensual",
                planBasicoAnual = "anual"
            ),


            // ------------------------------------------
            // PLAN HOGAR
            // ------------------------------------------

            PlanPremium(
                imagen = R.drawable.ic_plan_familiar,
                nombre = "Plan Hogar",
                precioMensual = "\$80 / mes",
                precioAnual = "\$880 / año",

                // ID del producto de Google Play
                productoId = "gourmeet_hogar",

                // Base plans de Google Play
                planBasicoMensual = "mensual",
                planBasicoAnual = "anual"
            )
        )


        // ==========================================
        // RECYCLER VIEW HORIZONTAL
        // ==========================================

        val layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        binding.planes.layoutManager = layoutManager


        // ==========================================
        // ADAPTER
        // ==========================================

        binding.planes.adapter = PlanPremiumAdapter(
            planes = planes
        ) { productoId, periodicidad, nombrePlan ->

            // --------------------------------------
            // POR AHORA SOLO COMPROBAMOS LA SELECCIÓN
            // --------------------------------------

            val mensaje = """
            Plan: $nombrePlan
            Producto: $productoId
            Periodicidad: $periodicidad
        """.trimIndent()

            android.util.Log.d(
                "GOURMEET_PLAN",
                mensaje
            )


            Toast.makeText(
                this,
                "$nombrePlan - $periodicidad",
                Toast.LENGTH_SHORT
            ).show()
        }


        // ==========================================
        // SNAP
        // ==========================================

        val snapHelper = PagerSnapHelper()

        snapHelper.attachToRecyclerView(
            binding.planes
        )
        Log.d("GOURMEET_BILLING", "Voy a iniciar Billing")
        iniciarFacturacion()

    }
    private fun iniciarFacturacion() {
        Log.d(
            "GOURMEET_BILLING",
            "Entrando a iniciarFacturacion()"
        )

        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->

                Log.d(
                    "GOURMEET_BILLING",
                    "Resultado de compra: ${billingResult.responseCode}"
                )
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        Log.d(
            "GOURMEET_BILLING",
            "Intentando conectar con Google Play..."
        )

        billingClient.startConnection(
            object : BillingClientStateListener {

                override fun onBillingSetupFinished(
                    billingResult: BillingResult
                ) {

                    if (
                        billingResult.responseCode ==
                        BillingClient.BillingResponseCode.OK
                    ) {

                        Log.d(
                            "GOURMEET_BILLING",
                            "Conexión con Google Play establecida"
                        )

                        consultarProductosPremium()

                    } else {

                        Log.e(
                            "GOURMEET_BILLING",
                            "Error al conectar: ${billingResult.debugMessage}"
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {

                    Log.w(
                        "GOURMEET_BILLING",
                        "Se perdió la conexión con Google Play"
                    )
                }
            }
        )
    }
    private fun consultarProductosPremium() {

        val listaProductos = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("gourmeet_individual")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),

            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("gourmeet_hogar")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val parametros = QueryProductDetailsParams.newBuilder()
            .setProductList(listaProductos)
            .build()

        billingClient.queryProductDetailsAsync(parametros) {
                billingResult,
                resultado ->

            if (
                billingResult.responseCode ==
                BillingClient.BillingResponseCode.OK
            ) {

                productosPremium.clear()

                resultado.productDetailsList.forEach { producto ->

                    productosPremium[producto.productId] = producto

                    Log.d(
                        "GOURMEET_BILLING",
                        "Producto encontrado: ${producto.productId}"
                    )

                    producto.subscriptionOfferDetails?.forEach { oferta ->

                        Log.d(
                            "GOURMEET_BILLING",
                            "Plan base: ${oferta.basePlanId}"
                        )
                    }
                }

                Log.d(
                    "GOURMEET_BILLING",
                    "Productos encontrados: ${productosPremium.size}"
                )

                resultado.unfetchedProductList.forEach { producto ->

                    Log.e(
                        "GOURMEET_BILLING",
                        "No se pudo consultar: ${producto.productId}. " +
                                "Código: ${producto.statusCode}"
                    )
                }

            } else {

                Log.e(
                    "GOURMEET_BILLING",
                    "Error al consultar productos: ${billingResult.debugMessage}"
                )
            }
        }
    }

}
