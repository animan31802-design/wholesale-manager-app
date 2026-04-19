package com.animan.wholesalemanager.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "wholesale_manager.db"
        const val DATABASE_VERSION = 1

        // ── Table names ──────────────────────────────────────────────
        const val TABLE_CUSTOMERS = "customers"
        const val TABLE_PRODUCTS = "products"
        const val TABLE_BILLS = "bills"
        const val TABLE_BILL_ITEMS = "bill_items"
        const val TABLE_LEDGER = "ledger"
        const val TABLE_EXPENSES = "expenses"

        // ── customers ────────────────────────────────────────────────
        const val COL_CUSTOMER_ID = "id"
        const val COL_CUSTOMER_NAME = "name"
        const val COL_CUSTOMER_PHONE = "phone"
        const val COL_CUSTOMER_ADDRESS = "address"
        const val COL_CUSTOMER_TOTAL_PURCHASE = "total_purchase"
        const val COL_CUSTOMER_TOTAL_PAID = "total_paid"
        const val COL_CUSTOMER_BALANCE = "balance"

        // ── products ─────────────────────────────────────────────────
        const val COL_PRODUCT_ID = "id"
        const val COL_PRODUCT_NAME = "name"
        const val COL_PRODUCT_PRICE = "price"
        const val COL_PRODUCT_QUANTITY = "quantity"
        const val COL_PRODUCT_CATEGORY = "category"

        // ── bills ────────────────────────────────────────────────────
        const val COL_BILL_ID = "id"
        const val COL_BILL_CUSTOMER_ID = "customer_id"
        const val COL_BILL_CUSTOMER_NAME = "customer_name"
        const val COL_BILL_ITEMS_TOTAL = "items_total"
        const val COL_BILL_PAID_AMOUNT = "paid_amount"
        const val COL_BILL_BALANCE = "balance"
        const val COL_BILL_TIMESTAMP = "timestamp"

        // ── bill_items ───────────────────────────────────────────────
        const val COL_BI_ID = "id"
        const val COL_BI_BILL_ID = "bill_id"
        const val COL_BI_PRODUCT_ID = "product_id"
        const val COL_BI_NAME = "name"
        const val COL_BI_PRICE = "price"
        const val COL_BI_QUANTITY = "quantity"

        // ── ledger ───────────────────────────────────────────────────
        const val COL_LEDGER_ID = "id"
        const val COL_LEDGER_CUSTOMER_ID = "customer_id"
        const val COL_LEDGER_AMOUNT = "amount"
        const val COL_LEDGER_TYPE = "type"           // CREDIT | PAYMENT
        const val COL_LEDGER_BILL_ID = "bill_id"
        const val COL_LEDGER_NOTE = "note"
        const val COL_LEDGER_TIMESTAMP = "timestamp"

        // ── expenses ─────────────────────────────────────────────────
        const val COL_EXPENSE_ID = "id"
        const val COL_EXPENSE_TITLE = "title"
        const val COL_EXPENSE_AMOUNT = "amount"
        const val COL_EXPENSE_DATE = "date"
    }

    // ─────────────────────────────────────────────────────────────────
    // onCreate — create all tables
    // ─────────────────────────────────────────────────────────────────
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_CUSTOMERS (
                $COL_CUSTOMER_ID        TEXT PRIMARY KEY,
                $COL_CUSTOMER_NAME      TEXT NOT NULL,
                $COL_CUSTOMER_PHONE     TEXT,
                $COL_CUSTOMER_ADDRESS   TEXT,
                $COL_CUSTOMER_TOTAL_PURCHASE REAL DEFAULT 0,
                $COL_CUSTOMER_TOTAL_PAID     REAL DEFAULT 0,
                $COL_CUSTOMER_BALANCE        REAL DEFAULT 0
            )"""
        )

        db.execSQL(
            """CREATE TABLE $TABLE_PRODUCTS (
                $COL_PRODUCT_ID       TEXT PRIMARY KEY,
                $COL_PRODUCT_NAME     TEXT NOT NULL,
                $COL_PRODUCT_PRICE    REAL NOT NULL,
                $COL_PRODUCT_QUANTITY INTEGER DEFAULT 0,
                $COL_PRODUCT_CATEGORY TEXT DEFAULT ''
            )"""
        )

        db.execSQL(
            """CREATE TABLE $TABLE_BILLS (
                $COL_BILL_ID            TEXT PRIMARY KEY,
                $COL_BILL_CUSTOMER_ID   TEXT NOT NULL,
                $COL_BILL_CUSTOMER_NAME TEXT NOT NULL,
                $COL_BILL_ITEMS_TOTAL   REAL DEFAULT 0,
                $COL_BILL_PAID_AMOUNT   REAL DEFAULT 0,
                $COL_BILL_BALANCE       REAL DEFAULT 0,
                $COL_BILL_TIMESTAMP     INTEGER NOT NULL,
                FOREIGN KEY($COL_BILL_CUSTOMER_ID) REFERENCES $TABLE_CUSTOMERS($COL_CUSTOMER_ID)
            )"""
        )

        db.execSQL(
            """CREATE TABLE $TABLE_BILL_ITEMS (
                $COL_BI_ID         TEXT PRIMARY KEY,
                $COL_BI_BILL_ID    TEXT NOT NULL,
                $COL_BI_PRODUCT_ID TEXT NOT NULL,
                $COL_BI_NAME       TEXT NOT NULL,
                $COL_BI_PRICE      REAL NOT NULL,
                $COL_BI_QUANTITY   INTEGER NOT NULL,
                FOREIGN KEY($COL_BI_BILL_ID) REFERENCES $TABLE_BILLS($COL_BILL_ID)
            )"""
        )

        db.execSQL(
            """CREATE TABLE $TABLE_LEDGER (
                $COL_LEDGER_ID          TEXT PRIMARY KEY,
                $COL_LEDGER_CUSTOMER_ID TEXT NOT NULL,
                $COL_LEDGER_AMOUNT      REAL NOT NULL,
                $COL_LEDGER_TYPE        TEXT NOT NULL,
                $COL_LEDGER_BILL_ID     TEXT DEFAULT '',
                $COL_LEDGER_NOTE        TEXT DEFAULT '',
                $COL_LEDGER_TIMESTAMP   INTEGER NOT NULL,
                FOREIGN KEY($COL_LEDGER_CUSTOMER_ID) REFERENCES $TABLE_CUSTOMERS($COL_CUSTOMER_ID)
            )"""
        )

        db.execSQL(
            """CREATE TABLE $TABLE_EXPENSES (
                $COL_EXPENSE_ID     TEXT PRIMARY KEY,
                $COL_EXPENSE_TITLE  TEXT NOT NULL,
                $COL_EXPENSE_AMOUNT REAL NOT NULL,
                $COL_EXPENSE_DATE   INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BILL_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BILLS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CUSTOMERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ─────────────────────────────────────────────────────────────────
    // CUSTOMERS
    // ─────────────────────────────────────────────────────────────────

    fun insertCustomer(customer: Customer): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_CUSTOMER_ID, customer.id)
            put(COL_CUSTOMER_NAME, customer.name)
            put(COL_CUSTOMER_PHONE, customer.phone)
            put(COL_CUSTOMER_ADDRESS, customer.address)
            put(COL_CUSTOMER_TOTAL_PURCHASE, customer.totalPurchase)
            put(COL_CUSTOMER_TOTAL_PAID, customer.totalPaid)
            put(COL_CUSTOMER_BALANCE, customer.balance)
        }
        return db.insert(TABLE_CUSTOMERS, null, cv) != -1L
    }

    fun getAllCustomers(): List<Customer> {
        val list = mutableListOf<Customer>()
        val db = readableDatabase
        val cursor = db.query(TABLE_CUSTOMERS, null, null, null, null, null, "$COL_CUSTOMER_NAME ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Customer(
                        id = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_NAME)),
                        phone = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_PHONE)) ?: "",
                        address = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_ADDRESS)) ?: "",
                        totalPurchase = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PURCHASE)),
                        totalPaid = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PAID)),
                        balance = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_BALANCE))
                    )
                )
            }
        }
        return list
    }

    fun getCustomerById(id: String): Customer? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CUSTOMERS, null,
            "$COL_CUSTOMER_ID = ?", arrayOf(id),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                Customer(
                    id = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_NAME)),
                    phone = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_PHONE)) ?: "",
                    address = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_ADDRESS)) ?: "",
                    totalPurchase = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PURCHASE)),
                    totalPaid = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PAID)),
                    balance = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_BALANCE))
                )
            } else null
        }
    }

    fun updateCustomer(customer: Customer): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_CUSTOMER_NAME, customer.name)
            put(COL_CUSTOMER_PHONE, customer.phone)
            put(COL_CUSTOMER_ADDRESS, customer.address)
            put(COL_CUSTOMER_TOTAL_PURCHASE, customer.totalPurchase)
            put(COL_CUSTOMER_TOTAL_PAID, customer.totalPaid)
            put(COL_CUSTOMER_BALANCE, customer.balance)
        }
        return db.update(TABLE_CUSTOMERS, cv, "$COL_CUSTOMER_ID = ?", arrayOf(customer.id)) > 0
    }

    fun deleteCustomer(id: String): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_CUSTOMERS, "$COL_CUSTOMER_ID = ?", arrayOf(id)) > 0
    }

    fun searchCustomers(query: String): List<Customer> {
        val list = mutableListOf<Customer>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CUSTOMERS, null,
            "$COL_CUSTOMER_NAME LIKE ? OR $COL_CUSTOMER_PHONE LIKE ?",
            arrayOf("%$query%", "%$query%"),
            null, null, "$COL_CUSTOMER_NAME ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Customer(
                        id = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_NAME)),
                        phone = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_PHONE)) ?: "",
                        address = it.getString(it.getColumnIndexOrThrow(COL_CUSTOMER_ADDRESS)) ?: "",
                        totalPurchase = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PURCHASE)),
                        totalPaid = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PAID)),
                        balance = it.getDouble(it.getColumnIndexOrThrow(COL_CUSTOMER_BALANCE))
                    )
                )
            }
        }
        return list
    }

    // ─────────────────────────────────────────────────────────────────
    // PRODUCTS
    // ─────────────────────────────────────────────────────────────────

    fun insertProduct(product: Product): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_PRODUCT_ID, product.id)
            put(COL_PRODUCT_NAME, product.name)
            put(COL_PRODUCT_PRICE, product.price)
            put(COL_PRODUCT_QUANTITY, product.quantity)
            put(COL_PRODUCT_CATEGORY, product.category)
        }
        return db.insert(TABLE_PRODUCTS, null, cv) != -1L
    }

    fun getAllProducts(): List<Product> {
        val list = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PRODUCTS, null, null, null, null, null, "$COL_PRODUCT_NAME ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Product(
                        id = it.getString(it.getColumnIndexOrThrow(COL_PRODUCT_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_PRODUCT_NAME)),
                        price = it.getDouble(it.getColumnIndexOrThrow(COL_PRODUCT_PRICE)),
                        quantity = it.getInt(it.getColumnIndexOrThrow(COL_PRODUCT_QUANTITY)),
                        category = it.getString(it.getColumnIndexOrThrow(COL_PRODUCT_CATEGORY)) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun updateProduct(product: Product): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_PRODUCT_NAME, product.name)
            put(COL_PRODUCT_PRICE, product.price)
            put(COL_PRODUCT_QUANTITY, product.quantity)
            put(COL_PRODUCT_CATEGORY, product.category)
        }
        return db.update(TABLE_PRODUCTS, cv, "$COL_PRODUCT_ID = ?", arrayOf(product.id)) > 0
    }

    fun deleteProduct(id: String): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_PRODUCTS, "$COL_PRODUCT_ID = ?", arrayOf(id)) > 0
    }

    fun decrementProductStock(productId: String, qty: Int): Boolean {
        val db = writableDatabase
        return db.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COL_PRODUCT_QUANTITY = $COL_PRODUCT_QUANTITY - ? WHERE $COL_PRODUCT_ID = ?",
            arrayOf(qty, productId)
        ).let { true }
    }

    fun incrementProductStock(productId: String, qty: Int): Boolean {
        val db = writableDatabase
        return db.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COL_PRODUCT_QUANTITY = $COL_PRODUCT_QUANTITY + ? WHERE $COL_PRODUCT_ID = ?",
            arrayOf(qty, productId)
        ).let { true }
    }

    fun searchProducts(query: String): List<Product> {
        val list = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PRODUCTS, null,
            "$COL_PRODUCT_NAME LIKE ?", arrayOf("%$query%"),
            null, null, "$COL_PRODUCT_NAME ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Product(
                        id = it.getString(it.getColumnIndexOrThrow(COL_PRODUCT_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_PRODUCT_NAME)),
                        price = it.getDouble(it.getColumnIndexOrThrow(COL_PRODUCT_PRICE)),
                        quantity = it.getInt(it.getColumnIndexOrThrow(COL_PRODUCT_QUANTITY)),
                        category = it.getString(it.getColumnIndexOrThrow(COL_PRODUCT_CATEGORY)) ?: ""
                    )
                )
            }
        }
        return list
    }

    // ─────────────────────────────────────────────────────────────────
    // BILLS + BILL ITEMS  (saved together in one transaction)
    // ─────────────────────────────────────────────────────────────────

    fun insertBillWithItems(bill: Bill): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val billCv = ContentValues().apply {
                put(COL_BILL_ID, bill.id)
                put(COL_BILL_CUSTOMER_ID, bill.customerId)
                put(COL_BILL_CUSTOMER_NAME, bill.customerName)
                put(COL_BILL_ITEMS_TOTAL, bill.itemsTotal)
                put(COL_BILL_PAID_AMOUNT, bill.paidAmount)
                put(COL_BILL_BALANCE, bill.balance)
                put(COL_BILL_TIMESTAMP, bill.timestamp)
            }
            db.insertOrThrow(TABLE_BILLS, null, billCv)

            bill.items.forEach { item ->
                val itemCv = ContentValues().apply {
                    put(COL_BI_ID, java.util.UUID.randomUUID().toString())
                    put(COL_BI_BILL_ID, bill.id)
                    put(COL_BI_PRODUCT_ID, item.productId)
                    put(COL_BI_NAME, item.name)
                    put(COL_BI_PRICE, item.price)
                    put(COL_BI_QUANTITY, item.quantity)
                }
                db.insertOrThrow(TABLE_BILL_ITEMS, null, itemCv)
            }

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getAllBills(): List<Bill> {
        val db = readableDatabase
        val bills = mutableListOf<Bill>()
        val cursor = db.query(
            TABLE_BILLS, null, null, null, null, null,
            "$COL_BILL_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val billId = it.getString(it.getColumnIndexOrThrow(COL_BILL_ID))
                bills.add(
                    Bill(
                        id = billId,
                        customerId = it.getString(it.getColumnIndexOrThrow(COL_BILL_CUSTOMER_ID)),
                        customerName = it.getString(it.getColumnIndexOrThrow(COL_BILL_CUSTOMER_NAME)),
                        itemsTotal = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_ITEMS_TOTAL)),
                        paidAmount = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_PAID_AMOUNT)),
                        balance = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_BALANCE)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_BILL_TIMESTAMP)),
                        items = getBillItems(billId)
                    )
                )
            }
        }
        return bills
    }

    fun getBillsByCustomer(customerId: String): List<Bill> {
        val db = readableDatabase
        val bills = mutableListOf<Bill>()
        val cursor = db.query(
            TABLE_BILLS, null,
            "$COL_BILL_CUSTOMER_ID = ?", arrayOf(customerId),
            null, null, "$COL_BILL_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val billId = it.getString(it.getColumnIndexOrThrow(COL_BILL_ID))
                bills.add(
                    Bill(
                        id = billId,
                        customerId = it.getString(it.getColumnIndexOrThrow(COL_BILL_CUSTOMER_ID)),
                        customerName = it.getString(it.getColumnIndexOrThrow(COL_BILL_CUSTOMER_NAME)),
                        itemsTotal = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_ITEMS_TOTAL)),
                        paidAmount = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_PAID_AMOUNT)),
                        balance = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_BALANCE)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_BILL_TIMESTAMP)),
                        items = getBillItems(billId)
                    )
                )
            }
        }
        return bills
    }

    fun getBillsByDateRange(from: Long, to: Long): List<Bill> {
        val db = readableDatabase
        val bills = mutableListOf<Bill>()
        val cursor = db.query(
            TABLE_BILLS, null,
            "$COL_BILL_TIMESTAMP BETWEEN ? AND ?",
            arrayOf(from.toString(), to.toString()),
            null, null, "$COL_BILL_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val billId = it.getString(it.getColumnIndexOrThrow(COL_BILL_ID))
                bills.add(
                    Bill(
                        id = billId,
                        customerId = it.getString(it.getColumnIndexOrThrow(COL_BILL_CUSTOMER_ID)),
                        customerName = it.getString(it.getColumnIndexOrThrow(COL_BILL_CUSTOMER_NAME)),
                        itemsTotal = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_ITEMS_TOTAL)),
                        paidAmount = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_PAID_AMOUNT)),
                        balance = it.getDouble(it.getColumnIndexOrThrow(COL_BILL_BALANCE)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_BILL_TIMESTAMP)),
                        items = getBillItems(billId)
                    )
                )
            }
        }
        return bills
    }

    private fun getBillItems(billId: String): List<BillItem> {
        val items = mutableListOf<BillItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BILL_ITEMS, null,
            "$COL_BI_BILL_ID = ?", arrayOf(billId),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                items.add(
                    BillItem(
                        productId = it.getString(it.getColumnIndexOrThrow(COL_BI_PRODUCT_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_BI_NAME)),
                        price = it.getDouble(it.getColumnIndexOrThrow(COL_BI_PRICE)),
                        quantity = it.getInt(it.getColumnIndexOrThrow(COL_BI_QUANTITY))
                    )
                )
            }
        }
        return items
    }

    // ─────────────────────────────────────────────────────────────────
    // LEDGER
    // ─────────────────────────────────────────────────────────────────

    fun insertLedgerEntry(ledger: Ledger): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_LEDGER_ID, ledger.id)
            put(COL_LEDGER_CUSTOMER_ID, ledger.customerId)
            put(COL_LEDGER_AMOUNT, ledger.amount)
            put(COL_LEDGER_TYPE, ledger.type)
            put(COL_LEDGER_BILL_ID, ledger.billId)
            put(COL_LEDGER_NOTE, ledger.note)
            put(COL_LEDGER_TIMESTAMP, ledger.timestamp)
        }
        return db.insert(TABLE_LEDGER, null, cv) != -1L
    }

    fun getLedgerByCustomer(customerId: String): List<Ledger> {
        val list = mutableListOf<Ledger>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LEDGER, null,
            "$COL_LEDGER_CUSTOMER_ID = ?", arrayOf(customerId),
            null, null, "$COL_LEDGER_TIMESTAMP DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Ledger(
                        id = it.getString(it.getColumnIndexOrThrow(COL_LEDGER_ID)),
                        customerId = it.getString(it.getColumnIndexOrThrow(COL_LEDGER_CUSTOMER_ID)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(COL_LEDGER_AMOUNT)),
                        type = it.getString(it.getColumnIndexOrThrow(COL_LEDGER_TYPE)),
                        billId = it.getString(it.getColumnIndexOrThrow(COL_LEDGER_BILL_ID)) ?: "",
                        note = it.getString(it.getColumnIndexOrThrow(COL_LEDGER_NOTE)) ?: "",
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_LEDGER_TIMESTAMP))
                    )
                )
            }
        }
        return list
    }

    // ─────────────────────────────────────────────────────────────────
    // EXPENSES
    // ─────────────────────────────────────────────────────────────────

    fun insertExpense(expense: Expense): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_EXPENSE_ID, expense.id)
            put(COL_EXPENSE_TITLE, expense.title)
            put(COL_EXPENSE_AMOUNT, expense.amount)
            put(COL_EXPENSE_DATE, expense.date)
        }
        return db.insert(TABLE_EXPENSES, null, cv) != -1L
    }

    fun getAllExpenses(): List<Expense> {
        val list = mutableListOf<Expense>()
        val db = readableDatabase
        val cursor = db.query(TABLE_EXPENSES, null, null, null, null, null, "$COL_EXPENSE_DATE DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Expense(
                        id = it.getString(it.getColumnIndexOrThrow(COL_EXPENSE_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_EXPENSE_TITLE)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(COL_EXPENSE_AMOUNT)),
                        date = it.getLong(it.getColumnIndexOrThrow(COL_EXPENSE_DATE))
                    )
                )
            }
        }
        return list
    }

    fun getExpensesByDateRange(from: Long, to: Long): List<Expense> {
        val list = mutableListOf<Expense>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EXPENSES, null,
            "$COL_EXPENSE_DATE BETWEEN ? AND ?",
            arrayOf(from.toString(), to.toString()),
            null, null, "$COL_EXPENSE_DATE DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Expense(
                        id = it.getString(it.getColumnIndexOrThrow(COL_EXPENSE_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_EXPENSE_TITLE)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(COL_EXPENSE_AMOUNT)),
                        date = it.getLong(it.getColumnIndexOrThrow(COL_EXPENSE_DATE))
                    )
                )
            }
        }
        return list
    }

    fun deleteExpense(id: String): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_EXPENSES, "$COL_EXPENSE_ID = ?", arrayOf(id)) > 0
    }
}