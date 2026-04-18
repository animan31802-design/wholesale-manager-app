package com.animan.wholesalemanager.repository

import com.animan.wholesalemanager.data.local.BillItem
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

    fun updateProductStock(
        items: List<BillItem>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        val batch = db.batch()

        items.forEach { item ->

            val productRef = db.collection("users")
                .document(userId!!)
                .collection("products")
                .document(item.productId)

            batch.update(productRef, "quantity", com.google.firebase.firestore.FieldValue.increment(-item.quantity.toLong()))
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Stock update failed") }
    }

    fun updateProduct(
        product: Product,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .document(userId!!)
            .collection("products")
            .document(product.id)
            .set(product)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }

    fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .document(userId!!)
            .collection("products")
            .document(productId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }
}