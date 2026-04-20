package com.animan.wholesalemanager.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "wholesale_manager.db"
        const val DATABASE_VERSION = 2          // bumped from 1 → 2

        const val TABLE_CUSTOMERS     = "customers"
        const val TABLE_PRODUCTS      = "products"
        const val TABLE_PRODUCTS_FTS  = "products_fts"   // Full-Text Search virtual table
        const val TABLE_BILLS         = "bills"
        const val TABLE_BILL_ITEMS    = "bill_items"
        const val TABLE_LEDGER        = "ledger"
        const val TABLE_EXPENSES      = "expenses"

        // ── customers ────────────────────────────────────────────────
        const val COL_CUSTOMER_ID             = "id"
        const val COL_CUSTOMER_NAME           = "name"
        const val COL_CUSTOMER_PHONE          = "phone"
        const val COL_CUSTOMER_ADDRESS        = "address"
        const val COL_CUSTOMER_TOTAL_PURCHASE = "total_purchase"
        const val COL_CUSTOMER_TOTAL_PAID     = "total_paid"
        const val COL_CUSTOMER_BALANCE        = "balance"

        // ── products ─────────────────────────────────────────────────
        const val COL_PRODUCT_ID            = "id"
        const val COL_PRODUCT_NAME          = "name"
        const val COL_PRODUCT_SELLING_PRICE = "selling_price"
        const val COL_PRODUCT_COST_PRICE    = "cost_price"
        const val COL_PRODUCT_QUANTITY      = "quantity"
        const val COL_PRODUCT_UNIT          = "unit"
        const val COL_PRODUCT_CATEGORY      = "category"
        const val COL_PRODUCT_MIN_STOCK     = "min_stock_level"
        const val COL_PRODUCT_BARCODE       = "barcode"

        // ── bills ────────────────────────────────────────────────────
        const val COL_BILL_ID            = "id"
        const val COL_BILL_CUSTOMER_ID   = "customer_id"
        const val COL_BILL_CUSTOMER_NAME = "customer_name"
        const val COL_BILL_ITEMS_TOTAL   = "items_total"
        const val COL_BILL_PAID_AMOUNT   = "paid_amount"
        const val COL_BILL_BALANCE       = "balance"
        const val COL_BILL_TIMESTAMP     = "timestamp"

        // ── bill_items ───────────────────────────────────────────────
        const val COL_BI_ID         = "id"
        const val COL_BI_BILL_ID    = "bill_id"
        const val COL_BI_PRODUCT_ID = "product_id"
        const val COL_BI_NAME       = "name"
        const val COL_BI_PRICE      = "price"
        const val COL_BI_COST_PRICE = "cost_price"
        const val COL_BI_UNIT       = "unit"
        const val COL_BI_QUANTITY   = "quantity"

        // ── ledger ───────────────────────────────────────────────────
        const val COL_LEDGER_ID          = "id"
        const val COL_LEDGER_CUSTOMER_ID = "customer_id"
        const val COL_LEDGER_AMOUNT      = "amount"
        const val COL_LEDGER_TYPE        = "type"
        const val COL_LEDGER_BILL_ID     = "bill_id"
        const val COL_LEDGER_NOTE        = "note"
        const val COL_LEDGER_TIMESTAMP   = "timestamp"

        // ── expenses ─────────────────────────────────────────────────
        const val COL_EXPENSE_ID     = "id"
        const val COL_EXPENSE_TITLE  = "title"
        const val COL_EXPENSE_AMOUNT = "amount"
        const val COL_EXPENSE_DATE   = "date"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createAllTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop everything and recreate — acceptable for dev phase
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS_FTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BILL_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BILLS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CUSTOMERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        onCreate(db)
    }

    private fun createAllTables(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE $TABLE_CUSTOMERS (
                $COL_CUSTOMER_ID             TEXT PRIMARY KEY,
                $COL_CUSTOMER_NAME           TEXT NOT NULL,
                $COL_CUSTOMER_PHONE          TEXT,
                $COL_CUSTOMER_ADDRESS        TEXT,
                $COL_CUSTOMER_TOTAL_PURCHASE REAL DEFAULT 0,
                $COL_CUSTOMER_TOTAL_PAID     REAL DEFAULT 0,
                $COL_CUSTOMER_BALANCE        REAL DEFAULT 0
            )""")

        db.execSQL("""
            CREATE TABLE $TABLE_PRODUCTS (
                $COL_PRODUCT_ID            TEXT PRIMARY KEY,
                $COL_PRODUCT_NAME          TEXT NOT NULL,
                $COL_PRODUCT_SELLING_PRICE REAL NOT NULL DEFAULT 0,
                $COL_PRODUCT_COST_PRICE    REAL NOT NULL DEFAULT 0,
                $COL_PRODUCT_QUANTITY      INTEGER DEFAULT 0,
                $COL_PRODUCT_UNIT          TEXT DEFAULT 'Piece',
                $COL_PRODUCT_CATEGORY      TEXT DEFAULT '',
                $COL_PRODUCT_MIN_STOCK     INTEGER DEFAULT 5,
                $COL_PRODUCT_BARCODE       TEXT DEFAULT ''
            )""")

        // FTS4 virtual table — mirrors name, category, barcode for instant full-text search
        // content="" makes it a contentless FTS table — we manually keep it in sync
        db.execSQL("""
            CREATE VIRTUAL TABLE $TABLE_PRODUCTS_FTS
            USING fts4(
                content="$TABLE_PRODUCTS",
                $COL_PRODUCT_ID,
                $COL_PRODUCT_NAME,
                $COL_PRODUCT_CATEGORY,
                $COL_PRODUCT_BARCODE
            )""")

        db.execSQL("""
            CREATE TABLE $TABLE_BILLS (
                $COL_BILL_ID            TEXT PRIMARY KEY,
                $COL_BILL_CUSTOMER_ID   TEXT NOT NULL,
                $COL_BILL_CUSTOMER_NAME TEXT NOT NULL,
                $COL_BILL_ITEMS_TOTAL   REAL DEFAULT 0,
                $COL_BILL_PAID_AMOUNT   REAL DEFAULT 0,
                $COL_BILL_BALANCE       REAL DEFAULT 0,
                $COL_BILL_TIMESTAMP     INTEGER NOT NULL,
                FOREIGN KEY($COL_BILL_CUSTOMER_ID) REFERENCES $TABLE_CUSTOMERS($COL_CUSTOMER_ID)
            )""")

        db.execSQL("""
            CREATE TABLE $TABLE_BILL_ITEMS (
                $COL_BI_ID         TEXT PRIMARY KEY,
                $COL_BI_BILL_ID    TEXT NOT NULL,
                $COL_BI_PRODUCT_ID TEXT NOT NULL,
                $COL_BI_NAME       TEXT NOT NULL,
                $COL_BI_PRICE      REAL NOT NULL,
                $COL_BI_COST_PRICE REAL DEFAULT 0,
                $COL_BI_UNIT       TEXT DEFAULT 'Piece',
                $COL_BI_QUANTITY   INTEGER NOT NULL,
                FOREIGN KEY($COL_BI_BILL_ID) REFERENCES $TABLE_BILLS($COL_BILL_ID)
            )""")

        db.execSQL("""
            CREATE TABLE $TABLE_LEDGER (
                $COL_LEDGER_ID          TEXT PRIMARY KEY,
                $COL_LEDGER_CUSTOMER_ID TEXT NOT NULL,
                $COL_LEDGER_AMOUNT      REAL NOT NULL,
                $COL_LEDGER_TYPE        TEXT NOT NULL,
                $COL_LEDGER_BILL_ID     TEXT DEFAULT '',
                $COL_LEDGER_NOTE        TEXT DEFAULT '',
                $COL_LEDGER_TIMESTAMP   INTEGER NOT NULL,
                FOREIGN KEY($COL_LEDGER_CUSTOMER_ID) REFERENCES $TABLE_CUSTOMERS($COL_CUSTOMER_ID)
            )""")

        db.execSQL("""
            CREATE TABLE $TABLE_EXPENSES (
                $COL_EXPENSE_ID     TEXT PRIMARY KEY,
                $COL_EXPENSE_TITLE  TEXT NOT NULL,
                $COL_EXPENSE_AMOUNT REAL NOT NULL,
                $COL_EXPENSE_DATE   INTEGER NOT NULL
            )""")

        // Indexes for fast lookups on billing screen
        db.execSQL("CREATE INDEX idx_products_category ON $TABLE_PRODUCTS($COL_PRODUCT_CATEGORY)")
        db.execSQL("CREATE INDEX idx_products_barcode  ON $TABLE_PRODUCTS($COL_PRODUCT_BARCODE)")
        db.execSQL("CREATE INDEX idx_bills_timestamp   ON $TABLE_BILLS($COL_BILL_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_bill_items_product ON $TABLE_BILL_ITEMS($COL_BI_PRODUCT_ID)")
    }

    // ─────────────────────────────────────────────────────────────────
    // CUSTOMERS
    // ─────────────────────────────────────────────────────────────────

    fun insertCustomer(customer: Customer): Boolean {
        val cv = ContentValues().apply {
            put(COL_CUSTOMER_ID, customer.id)
            put(COL_CUSTOMER_NAME, customer.name)
            put(COL_CUSTOMER_PHONE, customer.phone)
            put(COL_CUSTOMER_ADDRESS, customer.address)
            put(COL_CUSTOMER_TOTAL_PURCHASE, customer.totalPurchase)
            put(COL_CUSTOMER_TOTAL_PAID, customer.totalPaid)
            put(COL_CUSTOMER_BALANCE, customer.balance)
        }
        return writableDatabase.insert(TABLE_CUSTOMERS, null, cv) != -1L
    }

    fun getAllCustomers(): List<Customer> {
        val list = mutableListOf<Customer>()
        readableDatabase.query(TABLE_CUSTOMERS, null, null, null, null, null,
            "$COL_CUSTOMER_NAME ASC").use { c ->
            while (c.moveToNext()) list.add(c.toCustomer())
        }
        return list
    }

    fun getCustomerById(id: String): Customer? {
        readableDatabase.query(TABLE_CUSTOMERS, null,
            "$COL_CUSTOMER_ID=?", arrayOf(id), null, null, null).use { c ->
            if (c.moveToFirst()) return c.toCustomer()
        }
        return null
    }

    fun updateCustomer(customer: Customer): Boolean {
        val cv = ContentValues().apply {
            put(COL_CUSTOMER_NAME, customer.name)
            put(COL_CUSTOMER_PHONE, customer.phone)
            put(COL_CUSTOMER_ADDRESS, customer.address)
            put(COL_CUSTOMER_TOTAL_PURCHASE, customer.totalPurchase)
            put(COL_CUSTOMER_TOTAL_PAID, customer.totalPaid)
            put(COL_CUSTOMER_BALANCE, customer.balance)
        }
        return writableDatabase.update(TABLE_CUSTOMERS, cv,
            "$COL_CUSTOMER_ID=?", arrayOf(customer.id)) > 0
    }

    fun deleteCustomer(id: String): Boolean =
        writableDatabase.delete(TABLE_CUSTOMERS, "$COL_CUSTOMER_ID=?", arrayOf(id)) > 0

    fun searchCustomers(query: String): List<Customer> {
        val list = mutableListOf<Customer>()
        readableDatabase.query(TABLE_CUSTOMERS, null,
            "$COL_CUSTOMER_NAME LIKE ? OR $COL_CUSTOMER_PHONE LIKE ?",
            arrayOf("%$query%", "%$query%"), null, null, "$COL_CUSTOMER_NAME ASC").use { c ->
            while (c.moveToNext()) list.add(c.toCustomer())
        }
        return list
    }

    // ─────────────────────────────────────────────────────────────────
    // PRODUCTS  (with FTS sync)
    // ─────────────────────────────────────────────────────────────────

    fun insertProduct(product: Product): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val cv = product.toContentValues()
            val rowId = db.insert(TABLE_PRODUCTS, null, cv)
            if (rowId != -1L) {
                // Sync FTS
                db.execSQL(
                    "INSERT INTO $TABLE_PRODUCTS_FTS(rowid, $COL_PRODUCT_ID, $COL_PRODUCT_NAME, $COL_PRODUCT_CATEGORY, $COL_PRODUCT_BARCODE) VALUES(?,?,?,?,?)",
                    arrayOf(rowId, product.id, product.name, product.category, product.barcode)
                )
            }
            db.setTransactionSuccessful()
            rowId != -1L
        } catch (e: Exception) {
            e.printStackTrace(); false
        } finally {
            db.endTransaction()
        }
    }

    fun getAllProducts(): List<Product> {
        val list = mutableListOf<Product>()
        readableDatabase.query(TABLE_PRODUCTS, null, null, null, null, null,
            "$COL_PRODUCT_NAME ASC").use { c ->
            while (c.moveToNext()) list.add(c.toProduct())
        }
        return list
    }

    fun getProductsByCategory(category: String): List<Product> {
        val list = mutableListOf<Product>()
        readableDatabase.query(TABLE_PRODUCTS, null,
            "$COL_PRODUCT_CATEGORY=?", arrayOf(category), null, null,
            "$COL_PRODUCT_NAME ASC").use { c ->
            while (c.moveToNext()) list.add(c.toProduct())
        }
        return list
    }

    fun getDistinctCategories(): List<String> {
        val list = mutableListOf<String>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT $COL_PRODUCT_CATEGORY FROM $TABLE_PRODUCTS WHERE $COL_PRODUCT_CATEGORY != '' ORDER BY $COL_PRODUCT_CATEGORY ASC",
            null
        ).use { c ->
            while (c.moveToNext()) list.add(c.getString(0))
        }
        return list
    }

    // FTS-powered search — fast even with 1000+ products
    // Falls back to LIKE if FTS returns nothing (handles partial word matches)
    fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) return getAllProducts()

        val ftsResults = mutableListOf<Product>()
        try {
            // FTS match: append * for prefix matching ("sug" matches "sugar")
            readableDatabase.rawQuery(
                """SELECT p.* FROM $TABLE_PRODUCTS p
                   INNER JOIN $TABLE_PRODUCTS_FTS fts ON p.$COL_PRODUCT_ID = fts.$COL_PRODUCT_ID
                   WHERE $TABLE_PRODUCTS_FTS MATCH ?
                   ORDER BY p.$COL_PRODUCT_NAME ASC""",
                arrayOf("$query*")
            ).use { c ->
                while (c.moveToNext()) ftsResults.add(c.toProduct())
            }
        } catch (_: Exception) {}

        // Fallback LIKE search (catches mid-word matches FTS misses)
        if (ftsResults.isEmpty()) {
            readableDatabase.query(TABLE_PRODUCTS, null,
                "$COL_PRODUCT_NAME LIKE ? OR $COL_PRODUCT_CATEGORY LIKE ? OR $COL_PRODUCT_BARCODE LIKE ?",
                arrayOf("%$query%", "%$query%", "%$query%"),
                null, null, "$COL_PRODUCT_NAME ASC"
            ).use { c ->
                while (c.moveToNext()) ftsResults.add(c.toProduct())
            }
        }
        return ftsResults
    }

    fun getProductByBarcode(barcode: String): Product? {
        readableDatabase.query(TABLE_PRODUCTS, null,
            "$COL_PRODUCT_BARCODE=?", arrayOf(barcode), null, null, null).use { c ->
            if (c.moveToFirst()) return c.toProduct()
        }
        return null
    }

    // Returns top N most sold products based on bill_items history
    fun getFrequentlySoldProducts(limit: Int = 10): List<Product> {
        val list = mutableListOf<Product>()
        readableDatabase.rawQuery(
            """SELECT p.*, SUM(bi.$COL_BI_QUANTITY) as total_sold
               FROM $TABLE_PRODUCTS p
               INNER JOIN $TABLE_BILL_ITEMS bi ON p.$COL_PRODUCT_ID = bi.$COL_BI_PRODUCT_ID
               GROUP BY p.$COL_PRODUCT_ID
               ORDER BY total_sold DESC
               LIMIT ?""",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) list.add(c.toProduct())
        }
        return list
    }

    fun updateProduct(product: Product): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val rows = db.update(TABLE_PRODUCTS, product.toContentValues(),
                "$COL_PRODUCT_ID=?", arrayOf(product.id))
            // Rebuild FTS entry
            db.execSQL("DELETE FROM $TABLE_PRODUCTS_FTS WHERE $COL_PRODUCT_ID=?",
                arrayOf(product.id))
            db.execSQL(
                "INSERT INTO $TABLE_PRODUCTS_FTS($COL_PRODUCT_ID, $COL_PRODUCT_NAME, $COL_PRODUCT_CATEGORY, $COL_PRODUCT_BARCODE) VALUES(?,?,?,?)",
                arrayOf(product.id, product.name, product.category, product.barcode)
            )
            db.setTransactionSuccessful()
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace(); false
        } finally {
            db.endTransaction()
        }
    }

    fun deleteProduct(id: String): Boolean {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_PRODUCTS_FTS WHERE $COL_PRODUCT_ID=?", arrayOf(id))
        return db.delete(TABLE_PRODUCTS, "$COL_PRODUCT_ID=?", arrayOf(id)) > 0
    }

    fun decrementProductStock(productId: String, qty: Int) {
        writableDatabase.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COL_PRODUCT_QUANTITY = $COL_PRODUCT_QUANTITY - ? WHERE $COL_PRODUCT_ID = ?",
            arrayOf(qty, productId)
        )
    }

    fun incrementProductStock(productId: String, qty: Int) {
        writableDatabase.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COL_PRODUCT_QUANTITY = $COL_PRODUCT_QUANTITY + ? WHERE $COL_PRODUCT_ID = ?",
            arrayOf(qty, productId)
        )
    }

    fun getLowStockProducts(threshold: Int? = null): List<Product> {
        val list = mutableListOf<Product>()
        // Use per-product minStockLevel if no global threshold passed
        val sql = if (threshold != null)
            "SELECT * FROM $TABLE_PRODUCTS WHERE $COL_PRODUCT_QUANTITY <= $threshold ORDER BY $COL_PRODUCT_QUANTITY ASC"
        else
            "SELECT * FROM $TABLE_PRODUCTS WHERE $COL_PRODUCT_QUANTITY <= $COL_PRODUCT_MIN_STOCK ORDER BY $COL_PRODUCT_QUANTITY ASC"
        readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) list.add(c.toProduct())
        }
        return list
    }

    // ─────────────────────────────────────────────────────────────────
    // BILLS + BILL ITEMS
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
                val cv = ContentValues().apply {
                    put(COL_BI_ID, java.util.UUID.randomUUID().toString())
                    put(COL_BI_BILL_ID, bill.id)
                    put(COL_BI_PRODUCT_ID, item.productId)
                    put(COL_BI_NAME, item.name)
                    put(COL_BI_PRICE, item.price)
                    put(COL_BI_COST_PRICE, item.costPrice)
                    put(COL_BI_UNIT, item.unit)
                    put(COL_BI_QUANTITY, item.quantity)
                }
                db.insertOrThrow(TABLE_BILL_ITEMS, null, cv)
            }
            db.setTransactionSuccessful(); true
        } catch (e: Exception) {
            e.printStackTrace(); false
        } finally {
            db.endTransaction()
        }
    }

    fun getAllBills(): List<Bill> = queryBills(null, null)

    fun getBillsByDateRange(from: Long, to: Long): List<Bill> =
        queryBills("$COL_BILL_TIMESTAMP BETWEEN ? AND ?",
            arrayOf(from.toString(), to.toString()))

    fun getBillsByCustomer(customerId: String): List<Bill> =
        queryBills("$COL_BILL_CUSTOMER_ID=?", arrayOf(customerId))

    private fun queryBills(where: String?, args: Array<String>?): List<Bill> {
        val bills = mutableListOf<Bill>()
        readableDatabase.query(TABLE_BILLS, null, where, args, null, null,
            "$COL_BILL_TIMESTAMP DESC").use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_BILL_ID))
                bills.add(Bill(
                    id = id,
                    customerId = c.getString(c.getColumnIndexOrThrow(COL_BILL_CUSTOMER_ID)),
                    customerName = c.getString(c.getColumnIndexOrThrow(COL_BILL_CUSTOMER_NAME)),
                    itemsTotal = c.getDouble(c.getColumnIndexOrThrow(COL_BILL_ITEMS_TOTAL)),
                    paidAmount = c.getDouble(c.getColumnIndexOrThrow(COL_BILL_PAID_AMOUNT)),
                    balance = c.getDouble(c.getColumnIndexOrThrow(COL_BILL_BALANCE)),
                    timestamp = c.getLong(c.getColumnIndexOrThrow(COL_BILL_TIMESTAMP)),
                    items = getBillItems(id)
                ))
            }
        }
        return bills
    }

    private fun getBillItems(billId: String): List<BillItem> {
        val items = mutableListOf<BillItem>()
        readableDatabase.query(TABLE_BILL_ITEMS, null,
            "$COL_BI_BILL_ID=?", arrayOf(billId), null, null, null).use { c ->
            while (c.moveToNext()) {
                items.add(BillItem(
                    productId = c.getString(c.getColumnIndexOrThrow(COL_BI_PRODUCT_ID)),
                    name = c.getString(c.getColumnIndexOrThrow(COL_BI_NAME)),
                    price = c.getDouble(c.getColumnIndexOrThrow(COL_BI_PRICE)),
                    costPrice = c.getDouble(c.getColumnIndexOrThrow(COL_BI_COST_PRICE)),
                    unit = c.getString(c.getColumnIndexOrThrow(COL_BI_UNIT)),
                    quantity = c.getInt(c.getColumnIndexOrThrow(COL_BI_QUANTITY))
                ))
            }
        }
        return items
    }

    // ─────────────────────────────────────────────────────────────────
    // LEDGER
    // ─────────────────────────────────────────────────────────────────

    fun insertLedgerEntry(ledger: Ledger): Boolean {
        val cv = ContentValues().apply {
            put(COL_LEDGER_ID, ledger.id)
            put(COL_LEDGER_CUSTOMER_ID, ledger.customerId)
            put(COL_LEDGER_AMOUNT, ledger.amount)
            put(COL_LEDGER_TYPE, ledger.type)
            put(COL_LEDGER_BILL_ID, ledger.billId)
            put(COL_LEDGER_NOTE, ledger.note)
            put(COL_LEDGER_TIMESTAMP, ledger.timestamp)
        }
        return writableDatabase.insert(TABLE_LEDGER, null, cv) != -1L
    }

    fun getLedgerByCustomer(customerId: String): List<Ledger> {
        val list = mutableListOf<Ledger>()
        readableDatabase.query(TABLE_LEDGER, null,
            "$COL_LEDGER_CUSTOMER_ID=?", arrayOf(customerId), null, null,
            "$COL_LEDGER_TIMESTAMP DESC").use { c ->
            while (c.moveToNext()) {
                list.add(Ledger(
                    id = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_ID)),
                    customerId = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_CUSTOMER_ID)),
                    amount = c.getDouble(c.getColumnIndexOrThrow(COL_LEDGER_AMOUNT)),
                    type = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_TYPE)),
                    billId = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_BILL_ID)) ?: "",
                    note = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_NOTE)) ?: "",
                    timestamp = c.getLong(c.getColumnIndexOrThrow(COL_LEDGER_TIMESTAMP))
                ))
            }
        }
        return list
    }

    // ─────────────────────────────────────────────────────────────────
    // EXPENSES
    // ─────────────────────────────────────────────────────────────────

    fun insertExpense(expense: Expense): Boolean {
        val cv = ContentValues().apply {
            put(COL_EXPENSE_ID, expense.id)
            put(COL_EXPENSE_TITLE, expense.title)
            put(COL_EXPENSE_AMOUNT, expense.amount)
            put(COL_EXPENSE_DATE, expense.date)
        }
        return writableDatabase.insert(TABLE_EXPENSES, null, cv) != -1L
    }

    fun getAllExpenses(): List<Expense> {
        val list = mutableListOf<Expense>()
        readableDatabase.query(TABLE_EXPENSES, null, null, null, null, null,
            "$COL_EXPENSE_DATE DESC").use { c ->
            while (c.moveToNext()) list.add(c.toExpense())
        }
        return list
    }

    fun getExpensesByDateRange(from: Long, to: Long): List<Expense> {
        val list = mutableListOf<Expense>()
        readableDatabase.query(TABLE_EXPENSES, null,
            "$COL_EXPENSE_DATE BETWEEN ? AND ?",
            arrayOf(from.toString(), to.toString()), null, null,
            "$COL_EXPENSE_DATE DESC").use { c ->
            while (c.moveToNext()) list.add(c.toExpense())
        }
        return list
    }

    fun deleteExpense(id: String): Boolean =
        writableDatabase.delete(TABLE_EXPENSES, "$COL_EXPENSE_ID=?", arrayOf(id)) > 0

    // ─────────────────────────────────────────────────────────────────
    // Cursor extension helpers — keep query code clean
    // ─────────────────────────────────────────────────────────────────

    private fun android.database.Cursor.toCustomer() = Customer(
        id           = getString(getColumnIndexOrThrow(COL_CUSTOMER_ID)),
        name         = getString(getColumnIndexOrThrow(COL_CUSTOMER_NAME)),
        phone        = getString(getColumnIndexOrThrow(COL_CUSTOMER_PHONE)) ?: "",
        address      = getString(getColumnIndexOrThrow(COL_CUSTOMER_ADDRESS)) ?: "",
        totalPurchase= getDouble(getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PURCHASE)),
        totalPaid    = getDouble(getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PAID)),
        balance      = getDouble(getColumnIndexOrThrow(COL_CUSTOMER_BALANCE))
    )

    private fun android.database.Cursor.toProduct() = Product(
        id           = getString(getColumnIndexOrThrow(COL_PRODUCT_ID)),
        name         = getString(getColumnIndexOrThrow(COL_PRODUCT_NAME)),
        sellingPrice = getDouble(getColumnIndexOrThrow(COL_PRODUCT_SELLING_PRICE)),
        costPrice    = getDouble(getColumnIndexOrThrow(COL_PRODUCT_COST_PRICE)),
        quantity     = getInt(getColumnIndexOrThrow(COL_PRODUCT_QUANTITY)),
        unit         = getString(getColumnIndexOrThrow(COL_PRODUCT_UNIT)) ?: "Piece",
        category     = getString(getColumnIndexOrThrow(COL_PRODUCT_CATEGORY)) ?: "",
        minStockLevel= getInt(getColumnIndexOrThrow(COL_PRODUCT_MIN_STOCK)),
        barcode      = getString(getColumnIndexOrThrow(COL_PRODUCT_BARCODE)) ?: ""
    )

    private fun android.database.Cursor.toExpense() = Expense(
        id     = getString(getColumnIndexOrThrow(COL_EXPENSE_ID)),
        title  = getString(getColumnIndexOrThrow(COL_EXPENSE_TITLE)),
        amount = getDouble(getColumnIndexOrThrow(COL_EXPENSE_AMOUNT)),
        date   = getLong(getColumnIndexOrThrow(COL_EXPENSE_DATE))
    )

    private fun Product.toContentValues() = ContentValues().apply {
        put(COL_PRODUCT_ID, id)
        put(COL_PRODUCT_NAME, name)
        put(COL_PRODUCT_SELLING_PRICE, sellingPrice)
        put(COL_PRODUCT_COST_PRICE, costPrice)
        put(COL_PRODUCT_QUANTITY, quantity)
        put(COL_PRODUCT_UNIT, unit)
        put(COL_PRODUCT_CATEGORY, category)
        put(COL_PRODUCT_MIN_STOCK, minStockLevel)
        put(COL_PRODUCT_BARCODE, barcode)
    }
}