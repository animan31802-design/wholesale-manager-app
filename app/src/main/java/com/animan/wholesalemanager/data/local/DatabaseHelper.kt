package com.animan.wholesalemanager.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME    = "wholesale_manager.db"
        const val DATABASE_VERSION = 8  // bumped: Supplier Product Link Implementation

        const val TABLE_CUSTOMERS    = "customers"
        const val TABLE_PRODUCTS     = "products"
        const val TABLE_BILLS        = "bills"
        const val TABLE_BILL_ITEMS   = "bill_items"
        const val TABLE_LEDGER       = "ledger"
        const val TABLE_EXPENSES     = "expenses"

        // customers
        const val COL_CUSTOMER_ID             = "id"
        const val COL_CUSTOMER_NAME           = "name"
        const val COL_CUSTOMER_PHONE          = "phone"
        const val COL_CUSTOMER_ADDRESS        = "address"
        const val COL_CUSTOMER_LATITUDE       = "latitude"
        const val COL_CUSTOMER_LONGITUDE      = "longitude"
        const val COL_CUSTOMER_TOTAL_PURCHASE = "total_purchase"
        const val COL_CUSTOMER_TOTAL_PAID     = "total_paid"
        const val COL_CUSTOMER_BALANCE        = "balance"

        // products
        const val COL_PRODUCT_ID            = "id"
        const val COL_PRODUCT_NAME          = "name"
        const val COL_PRODUCT_SELLING_PRICE = "selling_price"
        const val COL_PRODUCT_COST_PRICE    = "cost_price"
        const val COL_PRODUCT_QUANTITY      = "quantity"
        const val COL_PRODUCT_UNIT          = "unit"
        const val COL_PRODUCT_CATEGORY      = "category"
        const val COL_PRODUCT_MIN_STOCK     = "min_stock_level"
        const val COL_PRODUCT_BARCODE       = "barcode"
        const val COL_PRODUCT_GST           = "gst_percent"
        const val COL_PRODUCT_ALLOW_PARTIAL = "allow_partial"

        // bills
        const val COL_BILL_ID            = "id"
        const val COL_BILL_CUSTOMER_ID   = "customer_id"
        const val COL_BILL_CUSTOMER_NAME = "customer_name"
        const val COL_BILL_ITEMS_TOTAL   = "items_total"
        const val COL_BILL_GST_TOTAL     = "gst_total"
        const val COL_BILL_GRAND_TOTAL   = "grand_total"
        const val COL_BILL_PAID_AMOUNT   = "paid_amount"
        const val COL_BILL_BALANCE       = "balance"
        const val COL_BILL_TIMESTAMP     = "timestamp"
        const val COL_BILL_IS_REFUNDED   = "is_refunded"
        const val COL_BILL_REFUNDED_AT   = "refunded_at"

        // bill_items
        const val COL_BI_ID         = "id"
        const val COL_BI_BILL_ID    = "bill_id"
        const val COL_BI_PRODUCT_ID = "product_id"
        const val COL_BI_NAME       = "name"
        const val COL_BI_PRICE      = "price"
        const val COL_BI_COST_PRICE = "cost_price"
        const val COL_BI_UNIT       = "unit"
        const val COL_BI_QUANTITY   = "quantity"
        const val COL_BI_GST        = "gst_percent"

        // ledger
        const val COL_LEDGER_ID          = "id"
        const val COL_LEDGER_CUSTOMER_ID = "customer_id"
        const val COL_LEDGER_AMOUNT      = "amount"
        const val COL_LEDGER_TYPE        = "type"
        const val COL_LEDGER_BILL_ID     = "bill_id"
        const val COL_LEDGER_NOTE        = "note"
        const val COL_LEDGER_TIMESTAMP   = "timestamp"

        // expenses
        const val COL_EXPENSE_ID     = "id"
        const val COL_EXPENSE_TITLE  = "title"
        const val COL_EXPENSE_AMOUNT = "amount"
        const val COL_EXPENSE_DATE   = "date"

        const val TABLE_SUPPLIERS        = "suppliers"
        const val TABLE_PURCHASES        = "purchases"
        const val TABLE_PURCHASE_ITEMS   = "purchase_items"
        const val TABLE_SUPPLIER_LEDGER  = "supplier_ledger"

        // suppliers
        const val COL_SUP_ID      = "id"
        const val COL_SUP_NAME    = "name"
        const val COL_SUP_PHONE   = "phone"
        const val COL_SUP_ADDRESS = "address"
        const val COL_SUP_BALANCE = "balance"
        const val COL_SUP_LATITUDE  = "latitude"
        const val COL_SUP_LONGITUDE = "longitude"

        const val TABLE_PRODUCT_SUPPLIERS  = "product_suppliers"
        const val COL_PS_ID                = "id"
        const val COL_PS_PRODUCT_ID        = "product_id"
        const val COL_PS_SUPPLIER_ID       = "supplier_id"
        const val COL_PS_SUPPLIER_NAME     = "supplier_name"
        const val COL_PS_COST_PRICE        = "cost_price"   // last known cost from this supplier
        const val COL_PS_NOTE              = "note"

        // purchases
        const val COL_PUR_ID            = "id"
        const val COL_PUR_SUPPLIER_ID   = "supplier_id"
        const val COL_PUR_SUPPLIER_NAME = "supplier_name"
        const val COL_PUR_PO_NUMBER     = "po_number"
        const val COL_PUR_ITEMS_TOTAL   = "items_total"
        const val COL_PUR_GST_TOTAL     = "gst_total"
        const val COL_PUR_GRAND_TOTAL   = "grand_total"
        const val COL_PUR_PAID_AMOUNT   = "paid_amount"
        const val COL_PUR_BALANCE       = "balance"
        const val COL_PUR_TIMESTAMP     = "timestamp"
        const val COL_PUR_NOTE          = "note"

        // purchase_items
        const val COL_PI_ID                 = "id"
        const val COL_PI_PURCHASE_ID        = "purchase_id"
        const val COL_PI_PRODUCT_ID         = "product_id"
        const val COL_PI_NAME               = "name"
        const val COL_PI_COST_PRICE         = "cost_price"
        const val COL_PI_PREV_COST_PRICE    = "previous_cost_price"
        const val COL_PI_QUANTITY           = "quantity"
        const val COL_PI_UNIT               = "unit"
        const val COL_PI_GST                = "gst_percent"

        // supplier_ledger
        const val COL_SL_ID          = "id"
        const val COL_SL_SUPPLIER_ID = "supplier_id"
        const val COL_SL_AMOUNT      = "amount"
        const val COL_SL_TYPE        = "type"
        const val COL_SL_PURCHASE_ID = "purchase_id"
        const val COL_SL_NOTE        = "note"
        const val COL_SL_TIMESTAMP   = "timestamp"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) = createAllTables(db)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            runCatching { db.execSQL("ALTER TABLE $TABLE_PRODUCTS ADD COLUMN $COL_PRODUCT_GST REAL DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_BILL_ITEMS ADD COLUMN $COL_BI_GST REAL DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_BILLS ADD COLUMN $COL_BILL_GST_TOTAL REAL DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_BILLS ADD COLUMN $COL_BILL_GRAND_TOTAL REAL DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_BILLS ADD COLUMN $COL_BILL_IS_REFUNDED INTEGER DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_BILLS ADD COLUMN $COL_BILL_REFUNDED_AT INTEGER DEFAULT 0") }
        }
        if (oldVersion < 4) {
            runCatching { db.execSQL("ALTER TABLE $TABLE_CUSTOMERS ADD COLUMN $COL_CUSTOMER_LATITUDE REAL DEFAULT NULL") }
            runCatching { db.execSQL("ALTER TABLE $TABLE_CUSTOMERS ADD COLUMN $COL_CUSTOMER_LONGITUDE REAL DEFAULT NULL") }
        }
        if (oldVersion < 5) {
            // Drop the FTS table — it was causing all product add/update failures
            runCatching { db.execSQL("DROP TABLE IF EXISTS products_fts") }
        }
        if (oldVersion < 6) {
            // products: quantity & min_stock → REAL, add allow_partial
            runCatching {
                db.execSQL("ALTER TABLE $TABLE_PRODUCTS ADD COLUMN allow_partial INTEGER DEFAULT 0")
            }
            // bill_items: quantity → REAL (SQLite stores as REAL when value has decimal)
            // No ALTER needed — SQLite REAL column accepts both int and decimal reads/writes
            // We just change how we read/write in Kotlin (getDouble instead of getInt)
        }
        if (oldVersion < 7) {
            runCatching {
                db.execSQL("""CREATE TABLE IF NOT EXISTS suppliers (
                id TEXT PRIMARY KEY, name TEXT NOT NULL,
                phone TEXT DEFAULT '', address TEXT DEFAULT '',
                latitude REAL, longitude REAL,
                balance REAL DEFAULT 0)""")
            }
            runCatching {
                db.execSQL("""CREATE TABLE IF NOT EXISTS purchases (
                id TEXT PRIMARY KEY, supplier_id TEXT NOT NULL,
                supplier_name TEXT NOT NULL, po_number TEXT DEFAULT '',
                items_total REAL DEFAULT 0, gst_total REAL DEFAULT 0,
                grand_total REAL DEFAULT 0, paid_amount REAL DEFAULT 0,
                balance REAL DEFAULT 0, timestamp INTEGER NOT NULL,
                note TEXT DEFAULT '')""")
            }
            runCatching {
                db.execSQL("""CREATE TABLE IF NOT EXISTS purchase_items (
                id TEXT PRIMARY KEY, purchase_id TEXT NOT NULL,
                product_id TEXT NOT NULL, name TEXT NOT NULL,
                cost_price REAL NOT NULL, previous_cost_price REAL DEFAULT 0,
                quantity REAL NOT NULL, unit TEXT DEFAULT 'Piece',
                gst_percent REAL DEFAULT 0)""")
            }
            runCatching {
                db.execSQL("""CREATE TABLE IF NOT EXISTS supplier_ledger (
                id TEXT PRIMARY KEY, supplier_id TEXT NOT NULL,
                amount REAL NOT NULL, type TEXT NOT NULL,
                purchase_id TEXT DEFAULT '', note TEXT DEFAULT '',
                timestamp INTEGER NOT NULL)""")
            }
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_purchases_supplier  ON purchases(supplier_id)") }
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_purchases_timestamp ON purchases(timestamp)") }
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_pi_purchase         ON purchase_items(purchase_id)") }
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_sl_supplier         ON supplier_ledger(supplier_id)") }
        }
        if (oldVersion < 8) {
            runCatching {
                db.execSQL("""CREATE TABLE IF NOT EXISTS product_suppliers (
            id TEXT PRIMARY KEY,
            product_id TEXT NOT NULL,
            supplier_id TEXT NOT NULL,
            supplier_name TEXT NOT NULL,
            cost_price REAL DEFAULT 0,
            note TEXT DEFAULT '')""")
            }
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_ps_product  ON product_suppliers(product_id)") }
            runCatching { db.execSQL("CREATE INDEX IF NOT EXISTS idx_ps_supplier ON product_suppliers(supplier_id)") }
        }
    }

    private fun createAllTables(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE $TABLE_CUSTOMERS (
            $COL_CUSTOMER_ID             TEXT PRIMARY KEY,
            $COL_CUSTOMER_NAME           TEXT NOT NULL,
            $COL_CUSTOMER_PHONE          TEXT,
            $COL_CUSTOMER_ADDRESS        TEXT,
            $COL_CUSTOMER_LATITUDE       REAL,
            $COL_CUSTOMER_LONGITUDE      REAL,
            $COL_CUSTOMER_TOTAL_PURCHASE REAL DEFAULT 0,
            $COL_CUSTOMER_TOTAL_PAID     REAL DEFAULT 0,
            $COL_CUSTOMER_BALANCE        REAL DEFAULT 0)""")

        db.execSQL("""CREATE TABLE $TABLE_PRODUCTS (
            $COL_PRODUCT_ID            TEXT PRIMARY KEY,
            $COL_PRODUCT_NAME          TEXT NOT NULL,
            $COL_PRODUCT_SELLING_PRICE REAL DEFAULT 0,
            $COL_PRODUCT_COST_PRICE    REAL DEFAULT 0,
            $COL_PRODUCT_QUANTITY      REAL DEFAULT 0,
            $COL_PRODUCT_UNIT          TEXT DEFAULT 'Piece',
            $COL_PRODUCT_CATEGORY      TEXT DEFAULT '',
            $COL_PRODUCT_MIN_STOCK     REAL DEFAULT 5,
            $COL_PRODUCT_BARCODE       TEXT DEFAULT '',
            $COL_PRODUCT_GST           REAL DEFAULT 0,
            $COL_PRODUCT_ALLOW_PARTIAL INTEGER DEFAULT 0)""")

        db.execSQL("""CREATE TABLE $TABLE_BILLS (
            $COL_BILL_ID            TEXT PRIMARY KEY,
            $COL_BILL_CUSTOMER_ID   TEXT NOT NULL,
            $COL_BILL_CUSTOMER_NAME TEXT NOT NULL,
            $COL_BILL_ITEMS_TOTAL   REAL DEFAULT 0,
            $COL_BILL_GST_TOTAL     REAL DEFAULT 0,
            $COL_BILL_GRAND_TOTAL   REAL DEFAULT 0,
            $COL_BILL_PAID_AMOUNT   REAL DEFAULT 0,
            $COL_BILL_BALANCE       REAL DEFAULT 0,
            $COL_BILL_TIMESTAMP     INTEGER NOT NULL,
            $COL_BILL_IS_REFUNDED   INTEGER DEFAULT 0,
            $COL_BILL_REFUNDED_AT   INTEGER DEFAULT 0,
            FOREIGN KEY($COL_BILL_CUSTOMER_ID) REFERENCES $TABLE_CUSTOMERS($COL_CUSTOMER_ID))""")

        db.execSQL("""CREATE TABLE $TABLE_BILL_ITEMS (
            $COL_BI_ID         TEXT PRIMARY KEY,
            $COL_BI_BILL_ID    TEXT NOT NULL,
            $COL_BI_PRODUCT_ID TEXT NOT NULL,
            $COL_BI_NAME       TEXT NOT NULL,
            $COL_BI_PRICE      REAL NOT NULL,
            $COL_BI_COST_PRICE REAL DEFAULT 0,
            $COL_BI_UNIT       TEXT DEFAULT 'Piece',
            $COL_BI_QUANTITY   REAL NOT NULL,
            $COL_BI_GST        REAL DEFAULT 0,
            FOREIGN KEY($COL_BI_BILL_ID) REFERENCES $TABLE_BILLS($COL_BILL_ID))""")

        db.execSQL("""CREATE TABLE $TABLE_LEDGER (
            $COL_LEDGER_ID          TEXT PRIMARY KEY,
            $COL_LEDGER_CUSTOMER_ID TEXT NOT NULL,
            $COL_LEDGER_AMOUNT      REAL NOT NULL,
            $COL_LEDGER_TYPE        TEXT NOT NULL,
            $COL_LEDGER_BILL_ID     TEXT DEFAULT '',
            $COL_LEDGER_NOTE        TEXT DEFAULT '',
            $COL_LEDGER_TIMESTAMP   INTEGER NOT NULL,
            FOREIGN KEY($COL_LEDGER_CUSTOMER_ID) REFERENCES $TABLE_CUSTOMERS($COL_CUSTOMER_ID))""")

        db.execSQL("""CREATE TABLE $TABLE_EXPENSES (
            $COL_EXPENSE_ID     TEXT PRIMARY KEY,
            $COL_EXPENSE_TITLE  TEXT NOT NULL,
            $COL_EXPENSE_AMOUNT REAL NOT NULL,
            $COL_EXPENSE_DATE   INTEGER NOT NULL)""")

        db.execSQL("""CREATE TABLE $TABLE_SUPPLIERS (
            $COL_SUP_ID      TEXT PRIMARY KEY,
            $COL_SUP_NAME    TEXT NOT NULL,
            $COL_SUP_PHONE   TEXT DEFAULT '',
            $COL_SUP_ADDRESS TEXT DEFAULT '',
            $COL_CUSTOMER_LATITUDE       REAL,
            $COL_CUSTOMER_LONGITUDE      REAL,
            $COL_SUP_BALANCE REAL DEFAULT 0)""")

        db.execSQL("""CREATE TABLE $TABLE_PURCHASES (
            $COL_PUR_ID            TEXT PRIMARY KEY,
            $COL_PUR_SUPPLIER_ID   TEXT NOT NULL,
            $COL_PUR_SUPPLIER_NAME TEXT NOT NULL,
            $COL_PUR_PO_NUMBER     TEXT DEFAULT '',
            $COL_PUR_ITEMS_TOTAL   REAL DEFAULT 0,
            $COL_PUR_GST_TOTAL     REAL DEFAULT 0,
            $COL_PUR_GRAND_TOTAL   REAL DEFAULT 0,
            $COL_PUR_PAID_AMOUNT   REAL DEFAULT 0,
            $COL_PUR_BALANCE       REAL DEFAULT 0,
            $COL_PUR_TIMESTAMP     INTEGER NOT NULL,
            $COL_PUR_NOTE          TEXT DEFAULT '',
            FOREIGN KEY($COL_PUR_SUPPLIER_ID) REFERENCES $TABLE_SUPPLIERS($COL_SUP_ID))""")

        db.execSQL("""CREATE TABLE $TABLE_PURCHASE_ITEMS (
            $COL_PI_ID              TEXT PRIMARY KEY,
            $COL_PI_PURCHASE_ID     TEXT NOT NULL,
            $COL_PI_PRODUCT_ID      TEXT NOT NULL,
            $COL_PI_NAME            TEXT NOT NULL,
            $COL_PI_COST_PRICE      REAL NOT NULL,
            $COL_PI_PREV_COST_PRICE REAL DEFAULT 0,
            $COL_PI_QUANTITY        REAL NOT NULL,
            $COL_PI_UNIT            TEXT DEFAULT 'Piece',
            $COL_PI_GST             REAL DEFAULT 0,
            FOREIGN KEY($COL_PI_PURCHASE_ID) REFERENCES $TABLE_PURCHASES($COL_PUR_ID))""")

        db.execSQL("""CREATE TABLE $TABLE_SUPPLIER_LEDGER (
            $COL_SL_ID          TEXT PRIMARY KEY,
            $COL_SL_SUPPLIER_ID TEXT NOT NULL,
            $COL_SL_AMOUNT      REAL NOT NULL,
            $COL_SL_TYPE        TEXT NOT NULL,
            $COL_SL_PURCHASE_ID TEXT DEFAULT '',
            $COL_SL_NOTE        TEXT DEFAULT '',
            $COL_SL_TIMESTAMP   INTEGER NOT NULL,
            FOREIGN KEY($COL_SL_SUPPLIER_ID) REFERENCES $TABLE_SUPPLIERS($COL_SUP_ID))""")

        db.execSQL("""CREATE TABLE $TABLE_PRODUCT_SUPPLIERS (
            $COL_PS_ID           TEXT PRIMARY KEY,
            $COL_PS_PRODUCT_ID   TEXT NOT NULL,
            $COL_PS_SUPPLIER_ID  TEXT NOT NULL,
            $COL_PS_SUPPLIER_NAME TEXT NOT NULL,
            $COL_PS_COST_PRICE   REAL DEFAULT 0,
            $COL_PS_NOTE         TEXT DEFAULT '',
            FOREIGN KEY($COL_PS_PRODUCT_ID)  REFERENCES $TABLE_PRODUCTS($COL_PRODUCT_ID),
            FOREIGN KEY($COL_PS_SUPPLIER_ID) REFERENCES $TABLE_SUPPLIERS($COL_SUP_ID))""")

        // Indexes for fast LIKE search — replaces FTS
        db.execSQL("CREATE INDEX idx_products_name     ON $TABLE_PRODUCTS($COL_PRODUCT_NAME)")
        db.execSQL("CREATE INDEX idx_products_category ON $TABLE_PRODUCTS($COL_PRODUCT_CATEGORY)")
        db.execSQL("CREATE INDEX idx_products_barcode  ON $TABLE_PRODUCTS($COL_PRODUCT_BARCODE)")
        db.execSQL("CREATE INDEX idx_bills_timestamp   ON $TABLE_BILLS($COL_BILL_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_bills_customer    ON $TABLE_BILLS($COL_BILL_CUSTOMER_ID)")
        db.execSQL("CREATE INDEX idx_bi_product        ON $TABLE_BILL_ITEMS($COL_BI_PRODUCT_ID)")
        db.execSQL("CREATE INDEX idx_purchases_supplier  ON $TABLE_PURCHASES($COL_PUR_SUPPLIER_ID)")
        db.execSQL("CREATE INDEX idx_purchases_timestamp ON $TABLE_PURCHASES($COL_PUR_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_pi_purchase         ON $TABLE_PURCHASE_ITEMS($COL_PI_PURCHASE_ID)")
        db.execSQL("CREATE INDEX idx_sl_supplier         ON $TABLE_SUPPLIER_LEDGER($COL_SL_SUPPLIER_ID)")
        db.execSQL("CREATE INDEX idx_ps_product  ON $TABLE_PRODUCT_SUPPLIERS($COL_PS_PRODUCT_ID)")
        db.execSQL("CREATE INDEX idx_ps_supplier ON $TABLE_PRODUCT_SUPPLIERS($COL_PS_SUPPLIER_ID)")
    }

    // ── CUSTOMERS ────────────────────────────────────────────────────

    fun insertCustomer(c: Customer) =
        writableDatabase.insert(TABLE_CUSTOMERS, null, c.toCV()) != -1L

    fun getAllCustomers(): List<Customer> = buildList {
        readableDatabase.query(TABLE_CUSTOMERS, null, null, null, null, null,
            "$COL_CUSTOMER_NAME ASC").use { while (it.moveToNext()) add(it.toCustomer()) }
    }

    fun getCustomerById(id: String): Customer? {
        readableDatabase.query(TABLE_CUSTOMERS, null, "$COL_CUSTOMER_ID=?",
            arrayOf(id), null, null, null).use {
            if (it.moveToFirst()) return it.toCustomer()
        }
        return null
    }

    fun updateCustomer(c: Customer): Boolean {
        val cv = ContentValues().apply {
            put(COL_CUSTOMER_NAME, c.name)
            put(COL_CUSTOMER_PHONE, c.phone)
            put(COL_CUSTOMER_ADDRESS, c.address)
            if (c.latitude != null) put(COL_CUSTOMER_LATITUDE, c.latitude)
            if (c.longitude != null) put(COL_CUSTOMER_LONGITUDE, c.longitude)
            put(COL_CUSTOMER_TOTAL_PURCHASE, c.totalPurchase)
            put(COL_CUSTOMER_TOTAL_PAID, c.totalPaid)
            put(COL_CUSTOMER_BALANCE, c.balance)
        }
        return writableDatabase.update(TABLE_CUSTOMERS, cv,
            "$COL_CUSTOMER_ID=?", arrayOf(c.id)) > 0
    }

    fun deleteCustomer(id: String) =
        writableDatabase.delete(TABLE_CUSTOMERS, "$COL_CUSTOMER_ID=?", arrayOf(id)) > 0

    fun searchCustomers(q: String): List<Customer> = buildList {
        readableDatabase.query(TABLE_CUSTOMERS, null,
            "$COL_CUSTOMER_NAME LIKE ? OR $COL_CUSTOMER_PHONE LIKE ?",
            arrayOf("%$q%", "%$q%"), null, null, "$COL_CUSTOMER_NAME ASC")
            .use { while (it.moveToNext()) add(it.toCustomer()) }
    }

    // ── PRODUCTS ─────────────────────────────────────────────────────
    // Simple and reliable — no FTS complexity

    fun insertProduct(p: Product): Boolean {
        val new = p.copy(id = if (p.id.isBlank()) java.util.UUID.randomUUID().toString() else p.id)
        return writableDatabase.insert(TABLE_PRODUCTS, null, new.toCV()) != -1L
    }

    fun getAllProducts(): List<Product> = buildList {
        readableDatabase.query(TABLE_PRODUCTS, null, null, null, null, null,
            "$COL_PRODUCT_NAME ASC").use { while (it.moveToNext()) add(it.toProduct()) }
    }

    fun getProductsByCategory(cat: String): List<Product> = buildList {
        readableDatabase.query(TABLE_PRODUCTS, null, "$COL_PRODUCT_CATEGORY=?",
            arrayOf(cat), null, null, "$COL_PRODUCT_NAME ASC")
            .use { while (it.moveToNext()) add(it.toProduct()) }
    }

    fun getDistinctCategories(): List<String> = buildList {
        readableDatabase.rawQuery(
            "SELECT DISTINCT $COL_PRODUCT_CATEGORY FROM $TABLE_PRODUCTS " +
                    "WHERE $COL_PRODUCT_CATEGORY != '' ORDER BY $COL_PRODUCT_CATEGORY ASC", null)
            .use { while (it.moveToNext()) add(it.getString(0)) }
    }

    fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) return getAllProducts()
        return buildList {
            readableDatabase.query(TABLE_PRODUCTS, null,
                "$COL_PRODUCT_NAME LIKE ? OR $COL_PRODUCT_CATEGORY LIKE ? OR $COL_PRODUCT_BARCODE LIKE ?",
                arrayOf("%$query%", "%$query%", "%$query%"),
                null, null, "$COL_PRODUCT_NAME ASC")
                .use { while (it.moveToNext()) add(it.toProduct()) }
        }
    }

    fun getProductByBarcode(barcode: String): Product? {
        readableDatabase.query(TABLE_PRODUCTS, null, "$COL_PRODUCT_BARCODE=?",
            arrayOf(barcode), null, null, null).use {
            if (it.moveToFirst()) return it.toProduct()
        }
        return null
    }

    fun getFrequentlySoldProducts(limit: Int = 10): List<Product> = buildList {
        readableDatabase.rawQuery(
            "SELECT p.*, SUM(bi.$COL_BI_QUANTITY) as sold FROM $TABLE_PRODUCTS p " +
                    "INNER JOIN $TABLE_BILL_ITEMS bi ON p.$COL_PRODUCT_ID = bi.$COL_BI_PRODUCT_ID " +
                    "GROUP BY p.$COL_PRODUCT_ID ORDER BY sold DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { while (it.moveToNext()) add(it.toProduct()) }
    }

    fun getLowStockProducts(): List<Product> = buildList {
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_PRODUCTS " +
                    "WHERE $COL_PRODUCT_QUANTITY <= $COL_PRODUCT_MIN_STOCK " +
                    "ORDER BY $COL_PRODUCT_QUANTITY ASC", null)
            .use { while (it.moveToNext()) add(it.toProduct()) }
    }

    fun updateProduct(p: Product): Boolean {
        // toUpdateCV() excludes id — never update the primary key
        return writableDatabase.update(TABLE_PRODUCTS, p.toUpdateCV(),
            "$COL_PRODUCT_ID = ?", arrayOf(p.id)) > 0
    }

    fun deleteProduct(id: String): Boolean =
        writableDatabase.delete(TABLE_PRODUCTS, "$COL_PRODUCT_ID = ?", arrayOf(id)) > 0

    fun decrementProductStock(productId: String, qty: Double) {
        writableDatabase.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COL_PRODUCT_QUANTITY = $COL_PRODUCT_QUANTITY - ? " +
                    "WHERE $COL_PRODUCT_ID = ?", arrayOf(qty, productId))
    }

    fun incrementProductStock(productId: String, qty: Double) {
        writableDatabase.execSQL(
            "UPDATE $TABLE_PRODUCTS SET $COL_PRODUCT_QUANTITY = $COL_PRODUCT_QUANTITY + ? " +
                    "WHERE $COL_PRODUCT_ID = ?", arrayOf(qty, productId))
    }

    // ── BILLS ────────────────────────────────────────────────────────

    fun insertBillWithItems(bill: Bill): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            db.insertOrThrow(TABLE_BILLS, null, ContentValues().apply {
                put(COL_BILL_ID, bill.id)
                put(COL_BILL_CUSTOMER_ID, bill.customerId)
                put(COL_BILL_CUSTOMER_NAME, bill.customerName)
                put(COL_BILL_ITEMS_TOTAL, bill.itemsTotal)
                put(COL_BILL_GST_TOTAL, bill.gstTotal)
                put(COL_BILL_GRAND_TOTAL, bill.grandTotal)
                put(COL_BILL_PAID_AMOUNT, bill.paidAmount)
                put(COL_BILL_BALANCE, bill.balance)
                put(COL_BILL_TIMESTAMP, bill.timestamp)
                put(COL_BILL_IS_REFUNDED, 0)
                put(COL_BILL_REFUNDED_AT, 0L)
            })
            bill.items.forEach { item ->
                db.insertOrThrow(TABLE_BILL_ITEMS, null, ContentValues().apply {
                    put(COL_BI_ID, java.util.UUID.randomUUID().toString())
                    put(COL_BI_BILL_ID, bill.id)
                    put(COL_BI_PRODUCT_ID, item.productId)
                    put(COL_BI_NAME, item.name)
                    put(COL_BI_PRICE, item.price)
                    put(COL_BI_COST_PRICE, item.costPrice)
                    put(COL_BI_UNIT, item.unit)
                    put(COL_BI_QUANTITY, item.quantity)
                    put(COL_BI_GST, item.gstPercent)
                })
            }
            db.setTransactionSuccessful(); true
        } catch (e: Exception) { e.printStackTrace(); false }
        finally { db.endTransaction() }
    }

    fun markBillRefunded(billId: String): Boolean {
        val cv = ContentValues().apply {
            put(COL_BILL_IS_REFUNDED, 1)
            put(COL_BILL_REFUNDED_AT, System.currentTimeMillis())
        }
        return writableDatabase.update(TABLE_BILLS, cv, "$COL_BILL_ID=?", arrayOf(billId)) > 0
    }

    fun getAllBills(): List<Bill>              = queryBills(null, null)
    fun getBillsByDateRange(f: Long, t: Long)  = queryBills("$COL_BILL_TIMESTAMP BETWEEN ? AND ?",
        arrayOf(f.toString(), t.toString()))
    fun getBillsByCustomer(id: String)         = queryBills("$COL_BILL_CUSTOMER_ID=?", arrayOf(id))

    private fun queryBills(where: String?, args: Array<String>?): List<Bill> = buildList {
        readableDatabase.query(TABLE_BILLS, null, where, args, null, null,
            "$COL_BILL_TIMESTAMP DESC").use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_BILL_ID))
                add(Bill(
                    id           = id,
                    customerId   = c.getString(c.getColumnIndexOrThrow(COL_BILL_CUSTOMER_ID)),
                    customerName = c.getString(c.getColumnIndexOrThrow(COL_BILL_CUSTOMER_NAME)),
                    itemsTotal   = c.getDouble(c.getColumnIndexOrThrow(COL_BILL_ITEMS_TOTAL)),
                    gstTotal     = c.getDouble(c.getColumnIndexOrThrow(COL_BILL_GST_TOTAL)),
                    grandTotal   = c.getDouble(c.getColumnIndexOrThrow(COL_BILL_GRAND_TOTAL)),
                    paidAmount   = c.getDouble(c.getColumnIndexOrThrow(COL_BILL_PAID_AMOUNT)),
                    balance      = c.getDouble(c.getColumnIndexOrThrow(COL_BILL_BALANCE)),
                    timestamp    = c.getLong(c.getColumnIndexOrThrow(COL_BILL_TIMESTAMP)),
                    isRefunded   = c.getInt(c.getColumnIndexOrThrow(COL_BILL_IS_REFUNDED)) == 1,
                    refundedAt   = c.getLong(c.getColumnIndexOrThrow(COL_BILL_REFUNDED_AT)),
                    items        = getBillItems(id)
                ))
            }
        }
    }

    private fun getBillItems(billId: String): List<BillItem> = buildList {
        readableDatabase.query(TABLE_BILL_ITEMS, null, "$COL_BI_BILL_ID=?",
            arrayOf(billId), null, null, null).use { c ->
            while (c.moveToNext()) add(BillItem(
                productId  = c.getString(c.getColumnIndexOrThrow(COL_BI_PRODUCT_ID)),
                name       = c.getString(c.getColumnIndexOrThrow(COL_BI_NAME)),
                price      = c.getDouble(c.getColumnIndexOrThrow(COL_BI_PRICE)),
                costPrice  = c.getDouble(c.getColumnIndexOrThrow(COL_BI_COST_PRICE)),
                unit       = c.getString(c.getColumnIndexOrThrow(COL_BI_UNIT)),
                quantity   = c.getDouble(c.getColumnIndexOrThrow(COL_BI_QUANTITY)),
                gstPercent = c.getDouble(c.getColumnIndexOrThrow(COL_BI_GST))
            ))
        }
    }

    // ── LEDGER ───────────────────────────────────────────────────────

    fun insertLedgerEntry(l: Ledger): Boolean {
        val cv = ContentValues().apply {
            put(COL_LEDGER_ID, l.id); put(COL_LEDGER_CUSTOMER_ID, l.customerId)
            put(COL_LEDGER_AMOUNT, l.amount); put(COL_LEDGER_TYPE, l.type)
            put(COL_LEDGER_BILL_ID, l.billId); put(COL_LEDGER_NOTE, l.note)
            put(COL_LEDGER_TIMESTAMP, l.timestamp)
        }
        return writableDatabase.insert(TABLE_LEDGER, null, cv) != -1L
    }

    fun getLedgerByCustomer(customerId: String): List<Ledger> = buildList {
        readableDatabase.query(TABLE_LEDGER, null, "$COL_LEDGER_CUSTOMER_ID=?",
            arrayOf(customerId), null, null, "$COL_LEDGER_TIMESTAMP DESC").use { c ->
            while (c.moveToNext()) add(Ledger(
                id         = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_ID)),
                customerId = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_CUSTOMER_ID)),
                amount     = c.getDouble(c.getColumnIndexOrThrow(COL_LEDGER_AMOUNT)),
                type       = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_TYPE)),
                billId     = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_BILL_ID)) ?: "",
                note       = c.getString(c.getColumnIndexOrThrow(COL_LEDGER_NOTE)) ?: "",
                timestamp  = c.getLong(c.getColumnIndexOrThrow(COL_LEDGER_TIMESTAMP))
            ))
        }
    }

    // ── EXPENSES ─────────────────────────────────────────────────────

    fun insertExpense(e: Expense): Boolean {
        val cv = ContentValues().apply {
            put(COL_EXPENSE_ID, e.id); put(COL_EXPENSE_TITLE, e.title)
            put(COL_EXPENSE_AMOUNT, e.amount); put(COL_EXPENSE_DATE, e.date)
        }
        return writableDatabase.insert(TABLE_EXPENSES, null, cv) != -1L
    }

    fun getAllExpenses(): List<Expense> = buildList {
        readableDatabase.query(TABLE_EXPENSES, null, null, null, null, null,
            "$COL_EXPENSE_DATE DESC").use { c ->
            while (c.moveToNext()) add(c.toExpense())
        }
    }

    fun getExpensesByDateRange(from: Long, to: Long): List<Expense> = buildList {
        readableDatabase.query(TABLE_EXPENSES, null,
            "$COL_EXPENSE_DATE BETWEEN ? AND ?",
            arrayOf(from.toString(), to.toString()), null, null, "$COL_EXPENSE_DATE DESC")
            .use { c -> while (c.moveToNext()) add(c.toExpense()) }
    }

    fun deleteExpense(id: String) =
        writableDatabase.delete(TABLE_EXPENSES, "$COL_EXPENSE_ID=?", arrayOf(id)) > 0

    // ── Cursor extensions ─────────────────────────────────────────────

    private fun android.database.Cursor.toCustomer() = Customer(
        id            = getString(getColumnIndexOrThrow(COL_CUSTOMER_ID)),
        name          = getString(getColumnIndexOrThrow(COL_CUSTOMER_NAME)),
        phone         = getString(getColumnIndexOrThrow(COL_CUSTOMER_PHONE)) ?: "",
        address       = getString(getColumnIndexOrThrow(COL_CUSTOMER_ADDRESS)) ?: "",
        latitude      = if (isNull(getColumnIndexOrThrow(COL_CUSTOMER_LATITUDE))) null
        else getDouble(getColumnIndexOrThrow(COL_CUSTOMER_LATITUDE)),
        longitude     = if (isNull(getColumnIndexOrThrow(COL_CUSTOMER_LONGITUDE))) null
        else getDouble(getColumnIndexOrThrow(COL_CUSTOMER_LONGITUDE)),
        totalPurchase = getDouble(getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PURCHASE)),
        totalPaid     = getDouble(getColumnIndexOrThrow(COL_CUSTOMER_TOTAL_PAID)),
        balance       = getDouble(getColumnIndexOrThrow(COL_CUSTOMER_BALANCE))
    )

    private fun android.database.Cursor.toProduct() = Product(
        id            = getString(getColumnIndexOrThrow(COL_PRODUCT_ID)),
        name          = getString(getColumnIndexOrThrow(COL_PRODUCT_NAME)),
        sellingPrice  = getDouble(getColumnIndexOrThrow(COL_PRODUCT_SELLING_PRICE)),
        costPrice     = getDouble(getColumnIndexOrThrow(COL_PRODUCT_COST_PRICE)),
        quantity      = getDouble(getColumnIndexOrThrow(COL_PRODUCT_QUANTITY)),  // ← getInt → getDouble
        unit          = getString(getColumnIndexOrThrow(COL_PRODUCT_UNIT)) ?: "Piece",
        category      = getString(getColumnIndexOrThrow(COL_PRODUCT_CATEGORY)) ?: "",
        minStockLevel = getDouble(getColumnIndexOrThrow(COL_PRODUCT_MIN_STOCK)), // ← getInt → getDouble
        barcode       = getString(getColumnIndexOrThrow(COL_PRODUCT_BARCODE)) ?: "",
        gstPercent    = getDouble(getColumnIndexOrThrow(COL_PRODUCT_GST)),
        allowPartial  = getInt(getColumnIndexOrThrow(COL_PRODUCT_ALLOW_PARTIAL)) == 1  // ← NEW
    )

    private fun android.database.Cursor.toExpense() = Expense(
        id     = getString(getColumnIndexOrThrow(COL_EXPENSE_ID)),
        title  = getString(getColumnIndexOrThrow(COL_EXPENSE_TITLE)),
        amount = getDouble(getColumnIndexOrThrow(COL_EXPENSE_AMOUNT)),
        date   = getLong(getColumnIndexOrThrow(COL_EXPENSE_DATE))
    )

    // ── ContentValues helpers ─────────────────────────────────────────

    private fun Customer.toCV() = ContentValues().apply {
        put(COL_CUSTOMER_ID, id); put(COL_CUSTOMER_NAME, name)
        put(COL_CUSTOMER_PHONE, phone); put(COL_CUSTOMER_ADDRESS, address)
        if (latitude != null) put(COL_CUSTOMER_LATITUDE, latitude)
        if (longitude != null) put(COL_CUSTOMER_LONGITUDE, longitude)
        put(COL_CUSTOMER_TOTAL_PURCHASE, totalPurchase)
        put(COL_CUSTOMER_TOTAL_PAID, totalPaid)
        put(COL_CUSTOMER_BALANCE, balance)
    }

    // Used for INSERT — includes id
    private fun Product.toCV() = ContentValues().apply {
        put(COL_PRODUCT_ID, id); put(COL_PRODUCT_NAME, name)
        put(COL_PRODUCT_SELLING_PRICE, sellingPrice); put(COL_PRODUCT_COST_PRICE, costPrice)
        put(COL_PRODUCT_QUANTITY, quantity)          // Double — SQLite handles it
        put(COL_PRODUCT_UNIT, unit)
        put(COL_PRODUCT_CATEGORY, category)
        put(COL_PRODUCT_MIN_STOCK, minStockLevel)    // Double
        put(COL_PRODUCT_BARCODE, barcode)
        put(COL_PRODUCT_GST, gstPercent)
        put(COL_PRODUCT_ALLOW_PARTIAL, if (allowPartial) 1 else 0)  // ← NEW
    }

    // Used for UPDATE — excludes id (never update primary key)
    private fun Product.toUpdateCV() = ContentValues().apply {
        put(COL_PRODUCT_NAME, name)
        put(COL_PRODUCT_SELLING_PRICE, sellingPrice); put(COL_PRODUCT_COST_PRICE, costPrice)
        put(COL_PRODUCT_QUANTITY, quantity)
        put(COL_PRODUCT_UNIT, unit)
        put(COL_PRODUCT_CATEGORY, category)
        put(COL_PRODUCT_MIN_STOCK, minStockLevel)
        put(COL_PRODUCT_BARCODE, barcode)
        put(COL_PRODUCT_GST, gstPercent)
        put(COL_PRODUCT_ALLOW_PARTIAL, if (allowPartial) 1 else 0)  // ← NEW
    }

    // ── SUPPLIERS ─────────────────────────────────────────────────────

    fun insertSupplier(s: Supplier): Boolean =
        writableDatabase.insert(TABLE_SUPPLIERS, null, s.toCV()) != -1L

    fun getAllSuppliers(): List<Supplier> = buildList {
        readableDatabase.query(TABLE_SUPPLIERS, null, null, null, null, null,
            "$COL_SUP_NAME ASC").use { while (it.moveToNext()) add(it.toSupplier()) }
    }

    fun getSupplierById(id: String): Supplier? {
        readableDatabase.query(TABLE_SUPPLIERS, null, "$COL_SUP_ID=?",
            arrayOf(id), null, null, null).use {
            if (it.moveToFirst()) return it.toSupplier()
        }
        return null
    }

    fun updateSupplier(s: Supplier): Boolean {
        val cv = ContentValues().apply {
            put(COL_SUP_NAME, s.name); put(COL_SUP_PHONE, s.phone)
            put(COL_SUP_ADDRESS, s.address); put(COL_SUP_BALANCE, s.balance)
        }
        return writableDatabase.update(TABLE_SUPPLIERS, cv,
            "$COL_SUP_ID=?", arrayOf(s.id)) > 0
    }

    fun deleteSupplier(id: String): Boolean =
        writableDatabase.delete(TABLE_SUPPLIERS, "$COL_SUP_ID=?", arrayOf(id)) > 0

    fun searchSuppliers(q: String): List<Supplier> = buildList {
        readableDatabase.query(TABLE_SUPPLIERS, null,
            "$COL_SUP_NAME LIKE ? OR $COL_SUP_PHONE LIKE ?",
            arrayOf("%$q%", "%$q%"), null, null, "$COL_SUP_NAME ASC")
            .use { while (it.moveToNext()) add(it.toSupplier()) }
    }

    // ── PURCHASES ─────────────────────────────────────────────────────

    fun insertPurchaseWithItems(purchase: Purchase): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            db.insertOrThrow(TABLE_PURCHASES, null, ContentValues().apply {
                put(COL_PUR_ID,            purchase.id)
                put(COL_PUR_SUPPLIER_ID,   purchase.supplierId)
                put(COL_PUR_SUPPLIER_NAME, purchase.supplierName)
                put(COL_PUR_PO_NUMBER,     purchase.poNumber)
                put(COL_PUR_ITEMS_TOTAL,   purchase.itemsTotal)
                put(COL_PUR_GST_TOTAL,     purchase.gstTotal)
                put(COL_PUR_GRAND_TOTAL,   purchase.grandTotal)
                put(COL_PUR_PAID_AMOUNT,   purchase.paidAmount)
                put(COL_PUR_BALANCE,       purchase.balance)
                put(COL_PUR_TIMESTAMP,     purchase.timestamp)
                put(COL_PUR_NOTE,          purchase.note)
            })

            purchase.items.forEach { item ->
                db.insertOrThrow(TABLE_PURCHASE_ITEMS, null, ContentValues().apply {
                    put(COL_PI_ID,              UUID.randomUUID().toString())
                    put(COL_PI_PURCHASE_ID,     purchase.id)
                    put(COL_PI_PRODUCT_ID,      item.productId)
                    put(COL_PI_NAME,            item.name)
                    put(COL_PI_COST_PRICE,      item.costPrice)
                    put(COL_PI_PREV_COST_PRICE, item.previousCostPrice)
                    put(COL_PI_QUANTITY,        item.quantity)
                    put(COL_PI_UNIT,            item.unit)
                    put(COL_PI_GST,             item.gstPercent)
                })
                // Increment stock
                incrementProductStock(item.productId, item.quantity)
                // Update cost price if changed
                if (item.costPrice != item.previousCostPrice) {
                    writableDatabase.execSQL("UPDATE $TABLE_PRODUCTS SET $COL_PRODUCT_COST_PRICE = ? " +
                            "WHERE $COL_PRODUCT_ID = ?",
                        arrayOf(item.costPrice, item.productId))
                }
            }

            db.setTransactionSuccessful(); true
        } catch (e: Exception) { e.printStackTrace(); false }
        finally { db.endTransaction() }
    }

    fun getAllPurchases(): List<Purchase> = queryPurchases(null, null)

    fun getPurchasesBySupplier(supplierId: String): List<Purchase> =
        queryPurchases("$COL_PUR_SUPPLIER_ID=?", arrayOf(supplierId))

    fun getPurchasesByDateRange(from: Long, to: Long): List<Purchase> =
        queryPurchases("$COL_PUR_TIMESTAMP BETWEEN ? AND ?",
            arrayOf(from.toString(), to.toString()))

    private fun queryPurchases(where: String?, args: Array<String>?): List<Purchase> = buildList {
        readableDatabase.query(TABLE_PURCHASES, null, where, args, null, null,
            "$COL_PUR_TIMESTAMP DESC").use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow(COL_PUR_ID))
                add(Purchase(
                    id           = id,
                    supplierId   = c.getString(c.getColumnIndexOrThrow(COL_PUR_SUPPLIER_ID)),
                    supplierName = c.getString(c.getColumnIndexOrThrow(COL_PUR_SUPPLIER_NAME)),
                    poNumber     = c.getString(c.getColumnIndexOrThrow(COL_PUR_PO_NUMBER)),
                    itemsTotal   = c.getDouble(c.getColumnIndexOrThrow(COL_PUR_ITEMS_TOTAL)),
                    gstTotal     = c.getDouble(c.getColumnIndexOrThrow(COL_PUR_GST_TOTAL)),
                    grandTotal   = c.getDouble(c.getColumnIndexOrThrow(COL_PUR_GRAND_TOTAL)),
                    paidAmount   = c.getDouble(c.getColumnIndexOrThrow(COL_PUR_PAID_AMOUNT)),
                    balance      = c.getDouble(c.getColumnIndexOrThrow(COL_PUR_BALANCE)),
                    timestamp    = c.getLong(c.getColumnIndexOrThrow(COL_PUR_TIMESTAMP)),
                    note         = c.getString(c.getColumnIndexOrThrow(COL_PUR_NOTE)) ?: "",
                    items        = getPurchaseItems(id)
                ))
            }
        }
    }

    private fun getPurchaseItems(purchaseId: String): List<PurchaseItem> = buildList {
        readableDatabase.query(TABLE_PURCHASE_ITEMS, null, "$COL_PI_PURCHASE_ID=?",
            arrayOf(purchaseId), null, null, null).use { c ->
            while (c.moveToNext()) add(PurchaseItem(
                productId         = c.getString(c.getColumnIndexOrThrow(COL_PI_PRODUCT_ID)),
                name              = c.getString(c.getColumnIndexOrThrow(COL_PI_NAME)),
                costPrice         = c.getDouble(c.getColumnIndexOrThrow(COL_PI_COST_PRICE)),
                previousCostPrice = c.getDouble(c.getColumnIndexOrThrow(COL_PI_PREV_COST_PRICE)),
                quantity          = c.getDouble(c.getColumnIndexOrThrow(COL_PI_QUANTITY)),
                unit              = c.getString(c.getColumnIndexOrThrow(COL_PI_UNIT)) ?: "Piece",
                gstPercent        = c.getDouble(c.getColumnIndexOrThrow(COL_PI_GST))
            ))
        }
    }

    // ── SUPPLIER LEDGER ───────────────────────────────────────────────

    fun insertSupplierLedgerEntry(l: SupplierLedger): Boolean {
        val cv = ContentValues().apply {
            put(COL_SL_ID,          l.id)
            put(COL_SL_SUPPLIER_ID, l.supplierId)
            put(COL_SL_AMOUNT,      l.amount)
            put(COL_SL_TYPE,        l.type)
            put(COL_SL_PURCHASE_ID, l.purchaseId)
            put(COL_SL_NOTE,        l.note)
            put(COL_SL_TIMESTAMP,   l.timestamp)
        }
        return writableDatabase.insert(TABLE_SUPPLIER_LEDGER, null, cv) != -1L
    }

    fun getSupplierLedger(supplierId: String): List<SupplierLedger> = buildList {
        readableDatabase.query(TABLE_SUPPLIER_LEDGER, null, "$COL_SL_SUPPLIER_ID=?",
            arrayOf(supplierId), null, null, "$COL_SL_TIMESTAMP DESC").use { c ->
            while (c.moveToNext()) add(SupplierLedger(
                id         = c.getString(c.getColumnIndexOrThrow(COL_SL_ID)),
                supplierId = c.getString(c.getColumnIndexOrThrow(COL_SL_SUPPLIER_ID)),
                amount     = c.getDouble(c.getColumnIndexOrThrow(COL_SL_AMOUNT)),
                type       = c.getString(c.getColumnIndexOrThrow(COL_SL_TYPE)),
                purchaseId = c.getString(c.getColumnIndexOrThrow(COL_SL_PURCHASE_ID)) ?: "",
                note       = c.getString(c.getColumnIndexOrThrow(COL_SL_NOTE)) ?: "",
                timestamp  = c.getLong(c.getColumnIndexOrThrow(COL_SL_TIMESTAMP))
            ))
        }
    }

    // ── Cursor extensions (add alongside existing ones) ───────────────

    private fun android.database.Cursor.toSupplier() = Supplier(
        id        = getString(getColumnIndexOrThrow(COL_SUP_ID)),
        name      = getString(getColumnIndexOrThrow(COL_SUP_NAME)),
        phone     = getString(getColumnIndexOrThrow(COL_SUP_PHONE)) ?: "",
        address   = getString(getColumnIndexOrThrow(COL_SUP_ADDRESS)) ?: "",
        balance   = getDouble(getColumnIndexOrThrow(COL_SUP_BALANCE)),
        latitude  = if (isNull(getColumnIndexOrThrow(COL_SUP_LATITUDE))) null
        else getDouble(getColumnIndexOrThrow(COL_SUP_LATITUDE)),
        longitude = if (isNull(getColumnIndexOrThrow(COL_SUP_LONGITUDE))) null
        else getDouble(getColumnIndexOrThrow(COL_SUP_LONGITUDE))
    )

    // ── ContentValues helpers (add alongside existing ones) ───────────
    private fun Supplier.toCV() = ContentValues().apply {
        put(COL_SUP_ID, id); put(COL_SUP_NAME, name)
        put(COL_SUP_PHONE, phone); put(COL_SUP_ADDRESS, address)
        put(COL_SUP_BALANCE, balance)
        if (latitude != null) put(COL_SUP_LATITUDE, latitude)
        if (longitude != null) put(COL_SUP_LONGITUDE, longitude)
    }

    fun reducePurchaseBalance(purchaseId: String, amount: Double) {
        writableDatabase.execSQL(
            "UPDATE $TABLE_PURCHASES SET " +
                    "$COL_PUR_PAID_AMOUNT = $COL_PUR_PAID_AMOUNT + ?, " +
                    "$COL_PUR_BALANCE = $COL_PUR_BALANCE - ? " +
                    "WHERE $COL_PUR_ID = ?",
            arrayOf(amount, amount, purchaseId)
        )
    }

    fun getSuppliersByProduct(productId: String): List<ProductSupplierLink> = buildList {
        readableDatabase.query(TABLE_PRODUCT_SUPPLIERS, null,
            "$COL_PS_PRODUCT_ID=?", arrayOf(productId),
            null, null, "$COL_PS_SUPPLIER_NAME ASC").use { c ->
            while (c.moveToNext()) add(ProductSupplierLink(
                id           = c.getString(c.getColumnIndexOrThrow(COL_PS_ID)),
                productId    = c.getString(c.getColumnIndexOrThrow(COL_PS_PRODUCT_ID)),
                supplierId   = c.getString(c.getColumnIndexOrThrow(COL_PS_SUPPLIER_ID)),
                supplierName = c.getString(c.getColumnIndexOrThrow(COL_PS_SUPPLIER_NAME)),
                costPrice    = c.getDouble(c.getColumnIndexOrThrow(COL_PS_COST_PRICE)),
                note         = c.getString(c.getColumnIndexOrThrow(COL_PS_NOTE)) ?: ""
            ))
        }
    }

    fun addProductSupplierLink(link: ProductSupplierLink): Boolean =
        writableDatabase.insert(TABLE_PRODUCT_SUPPLIERS, null, ContentValues().apply {
            put(COL_PS_ID,            link.id)
            put(COL_PS_PRODUCT_ID,    link.productId)
            put(COL_PS_SUPPLIER_ID,   link.supplierId)
            put(COL_PS_SUPPLIER_NAME, link.supplierName)
            put(COL_PS_COST_PRICE,    link.costPrice)
            put(COL_PS_NOTE,          link.note)
        }) != -1L

    fun removeProductSupplierLink(linkId: String): Boolean =
        writableDatabase.delete(TABLE_PRODUCT_SUPPLIERS,
            "$COL_PS_ID=?", arrayOf(linkId)) > 0

    fun updateProductSupplierLink(link: ProductSupplierLink): Boolean =
        writableDatabase.update(TABLE_PRODUCT_SUPPLIERS, ContentValues().apply {
            put(COL_PS_COST_PRICE, link.costPrice)
            put(COL_PS_NOTE,       link.note)
        }, "$COL_PS_ID=?", arrayOf(link.id)) > 0


    // ── clearAllData() — add new tables ───────────────────────────────
    // Update your existing clearAllData() to also clear new tables:
    fun clearAllData() {
        writableDatabase.apply {
            // Children first — tables with foreign keys pointing to others
            delete(TABLE_PRODUCT_SUPPLIERS, null, null)  // ← missing, add this
            delete(TABLE_PURCHASE_ITEMS,    null, null)
            delete(TABLE_SUPPLIER_LEDGER,   null, null)
            delete(TABLE_BILL_ITEMS,        null, null)
            delete(TABLE_LEDGER,            null, null)
            // Parents next
            delete(TABLE_PURCHASES,         null, null)
            delete(TABLE_BILLS,             null, null)
            delete(TABLE_EXPENSES,          null, null)
            // Root tables last
            delete(TABLE_SUPPLIERS,         null, null)
            delete(TABLE_CUSTOMERS,         null, null)
            delete(TABLE_PRODUCTS,          null, null)
        }
    }
}