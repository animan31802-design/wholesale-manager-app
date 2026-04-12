package com.animan.wholesalemanager.repository

import com.animan.wholesalemanager.data.local.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()

    fun addProduct(
        product: Product,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val id = db.collection("products").document().id
        val newProduct = product.copy(id = id)

        db.collection("users")
            .document(userId)
            .collection("products")
            .document(id)
            .set(newProduct)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }

    fun getProducts(
        onResult: (List<Product>) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("products")
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull {
                    it.toObject(Product::class.java)
                }
                onResult(list)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error")
            }
    }
}