package com.animan.wholesalemanager.utils

import android.content.Context

enum class Language { ENGLISH, TAMIL }

object AppLanguage {

    private const val PREF_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "app_language"

    var current: Language = Language.ENGLISH
        private set

    val strings: AppStrings get() = if (current == Language.TAMIL) TamilStrings else EnglishStrings

    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "ENGLISH")
        current = if (saved == "TAMIL") Language.TAMIL else Language.ENGLISH
    }

    fun setLanguage(context: Context, lang: Language) {
        current = lang
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, lang.name).apply()
    }
}

// ── String contract ───────────────────────────────────────────────────────────
abstract class AppStrings {
    // General
    abstract val save: String
    abstract val cancel: String
    abstract val update: String
    abstract val delete: String
    abstract val edit: String
    abstract val search: String
    abstract val confirm: String
    abstract val yes: String
    abstract val no: String
    abstract val back: String
    abstract val close: String
    abstract val loading: String
    abstract val error: String
    abstract val success: String
    abstract val noData: String

    // Auth
    abstract val login: String
    abstract val logout: String
    abstract val register: String
    abstract val email: String
    abstract val password: String
    abstract val confirmPassword: String
    abstract val loginSuccess: String

    // Dashboard
    abstract val dashboard: String
    abstract val todaySummary: String
    abstract val totalSales: String
    abstract val totalPaid: String
    abstract val pendingBalance: String
    abstract val profit: String
    abstract val expenses: String
    abstract val quickActions: String
    abstract val testPrint: String

    // Customers
    abstract val customers: String
    abstract val addCustomer: String
    abstract val editCustomer: String
    abstract val customerName: String
    abstract val phone: String
    abstract val address: String
    abstract val balance: String
    abstract val totalPurchase: String
    abstract val noCustomers: String
    abstract val deleteCustomer: String
    abstract val deleteCustomerConfirm: String
    abstract val bill: String
    abstract val ledger: String
    abstract val pay: String

    // Products
    abstract val products: String
    abstract val addProduct: String
    abstract val editProduct: String
    abstract val productName: String
    abstract val sellingPrice: String
    abstract val costPrice: String
    abstract val quantity: String
    abstract val unit: String
    abstract val category: String
    abstract val minStock: String
    abstract val barcode: String
    abstract val noGst: String
    abstract val outOfStock: String
    abstract val lowStock: String
    abstract val restock: String
    abstract val addStock: String
    abstract val nameEmpty: String
    abstract val invalidPrice: String
    abstract val noProducts: String

    // Billing
    abstract val billing: String
    abstract val createBill: String
    abstract val selectedItems: String
    abstract val itemsTotal: String
    abstract val gstTotal: String
    abstract val grandTotal: String
    abstract val previousBalance: String
    abstract val totalPayable: String
    abstract val paidAmount: String
    abstract val saveBill: String
    abstract val cart: String
    abstract val frequentlySold: String
    abstract val noProductsFound: String
    abstract val addAtLeastOne: String
    abstract val invalidAmount: String
    abstract val exceedsTotal: String
    abstract val billSaved: String
    abstract val fullAmount: String

    // Bill History
    abstract val billHistory: String
    abstract val reprint: String
    abstract val collect: String
    abstract val refund: String
    abstract val refundBill: String
    abstract val refundConfirm: String
    abstract val refunded: String
    abstract val noBills: String
    abstract val due: String

    // Ledger
    abstract val ledgerTitle: String
    abstract val noTransactions: String
    abstract val creditEntry: String
    abstract val paymentEntry: String
    abstract val refundEntry: String

    // Expenses
    abstract val expenseTitle: String
    abstract val addExpense: String
    abstract val expenseName: String
    abstract val amount: String
    abstract val totalExpenses: String
    abstract val noExpenses: String
    abstract val deleteExpense: String

    // Reports
    abstract val reports: String
    abstract val reportsDashboard: String
    abstract val businessInsights: String
    abstract val today: String
    abstract val week: String
    abstract val month: String
    abstract val all: String
    abstract val topProducts: String
    abstract val dailySales: String
    abstract val grossProfit: String
    abstract val netProfit: String
    abstract val insights: String

    // Settings
    abstract val settings: String
    abstract val shopDetails: String
    abstract val shopName: String
    abstract val upiPayment: String
    abstract val upiId: String
    abstract val saveUpiId: String
    abstract val printer: String
    abstract val selectPrinter: String
    abstract val cloudBackup: String
    abstract val enableBackup: String
    abstract val backupFrequency: String
    abstract val manualOnly: String
    abstract val daily: String
    abstract val weekly: String
    abstract val backupNow: String
    abstract val restore: String
    abstract val restoreFromBackup: String
    abstract val lastBackup: String
    abstract val language: String
    abstract val selectLanguage: String

    // Stock Consumption
    abstract val stockConsumption: String
    abstract val consumptionReason: String
    abstract val recordConsumption: String
    abstract val consumptionConfirm: String

    // Location
    abstract val useCurrentLocation: String
    abstract val pickOnMap: String
    abstract val selectLocation: String
    abstract val locationSelected: String

    // Payment
    abstract val recordPayment: String
    abstract val paymentAmount: String
    abstract val note: String
    abstract val outstandingBalance: String
    abstract val noBalance: String
    abstract val paymentRecorded: String
    abstract val quickFill: String

    // Printer
    abstract val selectPrinterTitle: String
    abstract val noPairedDevices: String
    abstract val savePrinterSelection: String
    abstract val clearSelection: String
    abstract val printerSaved: String
    abstract val retry: String

    // Bill PDF / Receipt
    abstract val invoice: String
    abstract val customer: String
    abstract val itemLabel: String
    abstract val qtyLabel: String
    abstract val priceLabel: String
    abstract val gstLabel: String
    abstract val totalLabel: String
    abstract val paidLabel: String
    abstract val balanceLabel: String
    abstract val thankYou: String
    abstract val date: String
}

// ── English strings ───────────────────────────────────────────────────────────
object EnglishStrings : AppStrings() {
    override val save = "Save"
    override val cancel = "Cancel"
    override val update = "Update"
    override val delete = "Delete"
    override val edit = "Edit"
    override val search = "Search"
    override val confirm = "Confirm"
    override val yes = "Yes"
    override val no = "No"
    override val back = "Back"
    override val close = "Close"
    override val loading = "Loading…"
    override val error = "Error"
    override val success = "Success"
    override val noData = "No data"

    override val login = "Login"
    override val logout = "Logout"
    override val register = "Register"
    override val email = "Email"
    override val password = "Password"
    override val confirmPassword = "Confirm password"
    override val loginSuccess = "Login successful"

    override val dashboard = "Dashboard"
    override val todaySummary = "Today's summary"
    override val totalSales = "Total sales"
    override val totalPaid = "Total paid"
    override val pendingBalance = "Pending balance"
    override val profit = "Profit"
    override val expenses = "Expenses"
    override val quickActions = "Quick actions"
    override val testPrint = "Test print"

    override val customers = "Customers"
    override val addCustomer = "Add customer"
    override val editCustomer = "Edit customer"
    override val customerName = "Customer name"
    override val phone = "Phone"
    override val address = "Address"
    override val balance = "Balance"
    override val totalPurchase = "Total purchase"
    override val noCustomers = "No customers yet"
    override val deleteCustomer = "Delete customer"
    override val deleteCustomerConfirm = "Delete this customer? This cannot be undone."
    override val bill = "Bill"
    override val ledger = "Ledger"
    override val pay = "Pay"

    override val products = "Products"
    override val addProduct = "Add product"
    override val editProduct = "Edit product"
    override val productName = "Product name"
    override val sellingPrice = "Selling price"
    override val costPrice = "Cost price"
    override val quantity = "Quantity"
    override val unit = "Unit"
    override val category = "Category"
    override val minStock = "Min stock alert"
    override val barcode = "Barcode (optional)"
    override val noGst = "No GST"
    override val outOfStock = "Out of stock"
    override val lowStock = "Low stock"
    override val restock = "Restock"
    override val addStock = "Add stock"
    override val nameEmpty = "Name cannot be empty"
    override val invalidPrice = "Enter a valid price"
    override val noProducts = "No products yet"

    override val billing = "Billing"
    override val createBill = "Create bill"
    override val selectedItems = "Selected items"
    override val itemsTotal = "Items total"
    override val gstTotal = "GST"
    override val grandTotal = "Grand total"
    override val previousBalance = "Previous balance"
    override val totalPayable = "Total payable"
    override val paidAmount = "Paid amount (₹)"
    override val saveBill = "Save bill"
    override val cart = "Cart"
    override val frequentlySold = "Frequently sold"
    override val noProductsFound = "No products found"
    override val addAtLeastOne = "Add at least one product"
    override val invalidAmount = "Enter a valid amount"
    override val exceedsTotal = "Amount exceeds total"
    override val billSaved = "Bill saved"
    override val fullAmount = "Full"

    override val billHistory = "Bill history"
    override val reprint = "Reprint"
    override val collect = "Collect"
    override val refund = "Refund"
    override val refundBill = "Refund bill"
    override val refundConfirm = "This will reverse the bill, restock all items and adjust the customer balance. Cannot be undone."
    override val refunded = "Refunded"
    override val noBills = "No bills yet"
    override val due = "Due"

    override val ledgerTitle = "Ledger"
    override val noTransactions = "No transactions yet"
    override val creditEntry = "Bill"
    override val paymentEntry = "Payment"
    override val refundEntry = "Refund"

    override val expenseTitle = "Expenses"
    override val addExpense = "Add expense"
    override val expenseName = "Expense name"
    override val amount = "Amount"
    override val totalExpenses = "Total expenses"
    override val noExpenses = "No expenses recorded"
    override val deleteExpense = "Delete expense"

    override val reports = "Reports"
    override val reportsDashboard = "Reports Dashboard"
    override val businessInsights = "Your business insights"
    override val today = "Today"
    override val week = "Week"
    override val month = "Month"
    override val all = "All"
    override val topProducts = "Top Products"
    override val dailySales = "Daily Sales"
    override val grossProfit = "Gross profit"
    override val netProfit = "Net profit"
    override val insights = "Insights"

    override val settings = "Settings"
    override val shopDetails = "Shop details"
    override val shopName = "Shop name"
    override val upiPayment = "UPI payment"
    override val upiId = "UPI ID"
    override val saveUpiId = "Save UPI ID"
    override val printer = "Printer"
    override val selectPrinter = "Select / change Bluetooth printer"
    override val cloudBackup = "Cloud backup"
    override val enableBackup = "Enable automatic backup"
    override val backupFrequency = "Backup frequency"
    override val manualOnly = "Manual only"
    override val daily = "Daily"
    override val weekly = "Weekly"
    override val backupNow = "Backup now"
    override val restore = "Restore"
    override val restoreFromBackup = "Restore from last backup"
    override val lastBackup = "Last backup"
    override val language = "Language"
    override val selectLanguage = "Select language"

    override val stockConsumption = "Stock consumption"
    override val consumptionReason = "Reason (e.g. Internal use, Damaged)"
    override val recordConsumption = "Record"
    override val consumptionConfirm = "Reduce stock and record as expense?"

    override val useCurrentLocation = "Use current location"
    override val pickOnMap = "Pick on map"
    override val selectLocation = "Select location"
    override val locationSelected = "Location selected"

    override val recordPayment = "Record payment"
    override val paymentAmount = "Payment amount (₹)"
    override val note = "Note (optional)"
    override val outstandingBalance = "Outstanding balance"
    override val noBalance = "No outstanding balance"
    override val paymentRecorded = "Payment recorded"
    override val quickFill = "Quick fill"

    override val selectPrinterTitle = "Select printer"
    override val noPairedDevices = "No paired Bluetooth devices found. Pair your printer in Android Settings first."
    override val savePrinterSelection = "Save printer selection"
    override val clearSelection = "Clear selection"
    override val printerSaved = "Printer saved"
    override val retry = "Retry"

    override val invoice = "Tax Invoice"
    override val customer = "Customer"
    override val itemLabel = "Item"
    override val qtyLabel = "Qty"
    override val priceLabel = "Price"
    override val gstLabel = "GST"
    override val totalLabel = "Total"
    override val paidLabel = "Paid"
    override val balanceLabel = "Balance"
    override val thankYou = "Thank You! Visit Again"
    override val date = "Date"
}

// ── Tamil strings ─────────────────────────────────────────────────────────────
object TamilStrings : AppStrings() {
    override val save = "சேமி"
    override val cancel = "ரத்து செய்"
    override val update = "புதுப்பி"
    override val delete = "நீக்கு"
    override val edit = "திருத்து"
    override val search = "தேடு"
    override val confirm = "உறுதிப்படுத்து"
    override val yes = "ஆம்"
    override val no = "இல்லை"
    override val back = "பின்னால்"
    override val close = "மூடு"
    override val loading = "ஏற்றுகிறது…"
    override val error = "பிழை"
    override val success = "வெற்றி"
    override val noData = "தகவல் இல்லை"

    override val login = "உள்நுழை"
    override val logout = "வெளியேறு"
    override val register = "பதிவு செய்"
    override val email = "மின்னஞ்சல்"
    override val password = "கடவுச்சொல்"
    override val confirmPassword = "கடவுச்சொல் உறுதிப்படுத்து"
    override val loginSuccess = "உள்நுழைவு வெற்றி"

    override val dashboard = "முகப்பு"
    override val todaySummary = "இன்றைய சுருக்கம்"
    override val totalSales = "மொத்த விற்பனை"
    override val totalPaid = "மொத்த செலுத்தல்"
    override val pendingBalance = "நிலுவை தொகை"
    override val profit = "லாபம்"
    override val expenses = "செலவுகள்"
    override val quickActions = "விரைவு செயல்கள்"
    override val testPrint = "சோதனை அச்சு"

    override val customers = "வாடிக்கையாளர்கள்"
    override val addCustomer = "வாடிக்கையாளர் சேர்"
    override val editCustomer = "வாடிக்கையாளர் திருத்து"
    override val customerName = "வாடிக்கையாளர் பெயர்"
    override val phone = "தொலைபேசி"
    override val address = "முகவரி"
    override val balance = "நிலுவை"
    override val totalPurchase = "மொத்த வாங்கல்"
    override val noCustomers = "வாடிக்கையாளர்கள் இல்லை"
    override val deleteCustomer = "வாடிக்கையாளர் நீக்கு"
    override val deleteCustomerConfirm = "இந்த வாடிக்கையாளரை நீக்கவா? இதை மீட்டெடுக்க முடியாது."
    override val bill = "பில்"
    override val ledger = "கணக்கு"
    override val pay = "செலுத்து"

    override val products = "பொருட்கள்"
    override val addProduct = "பொருள் சேர்"
    override val editProduct = "பொருள் திருத்து"
    override val productName = "பொருளின் பெயர்"
    override val sellingPrice = "விற்பனை விலை"
    override val costPrice = "கொள்முதல் விலை"
    override val quantity = "அளவு"
    override val unit = "அலகு"
    override val category = "வகை"
    override val minStock = "குறைந்தபட்ச இருப்பு எச்சரிக்கை"
    override val barcode = "பாரகோட் (விருப்பம்)"
    override val noGst = "GST இல்லை"
    override val outOfStock = "இருப்பு இல்லை"
    override val lowStock = "குறைந்த இருப்பு"
    override val restock = "மறுசேமி"
    override val addStock = "இருப்பு சேர்"
    override val nameEmpty = "பெயர் காலியாக இருக்கக்கூடாது"
    override val invalidPrice = "சரியான விலை உள்ளிடவும்"
    override val noProducts = "பொருட்கள் இல்லை"

    override val billing = "பில்லிங்"
    override val createBill = "பில் உருவாக்கு"
    override val selectedItems = "தேர்ந்தெடுத்த பொருட்கள்"
    override val itemsTotal = "பொருட்கள் மொத்தம்"
    override val gstTotal = "GST"
    override val grandTotal = "மொத்த தொகை"
    override val previousBalance = "முந்தைய நிலுவை"
    override val totalPayable = "செலுத்த வேண்டிய மொத்தம்"
    override val paidAmount = "செலுத்திய தொகை (₹)"
    override val saveBill = "பில் சேமி"
    override val cart = "கார்ட்"
    override val frequentlySold = "அடிக்கடி விற்பனை"
    override val noProductsFound = "பொருட்கள் கிடைக்கவில்லை"
    override val addAtLeastOne = "குறைந்தது ஒரு பொருள் சேர்க்கவும்"
    override val invalidAmount = "சரியான தொகை உள்ளிடவும்"
    override val exceedsTotal = "தொகை மொத்தத்தை தாண்டுகிறது"
    override val billSaved = "பில் சேமிக்கப்பட்டது"
    override val fullAmount = "முழு தொகை"

    override val billHistory = "பில் வரலாறு"
    override val reprint = "மீண்டும் அச்சு"
    override val collect = "வசூலி"
    override val refund = "திரும்ப"
    override val refundBill = "பில் திரும்பப்பெறு"
    override val refundConfirm = "பில் ரத்து செய்யப்படும், பொருட்கள் இருப்பில் சேர்க்கப்படும். இதை மீட்க முடியாது."
    override val refunded = "திரும்பப்பெறப்பட்டது"
    override val noBills = "பில்கள் இல்லை"
    override val due = "நிலுவை"

    override val ledgerTitle = "கணக்கு பட்டியல்"
    override val noTransactions = "பரிவர்த்தனைகள் இல்லை"
    override val creditEntry = "பில்"
    override val paymentEntry = "செலுத்தல்"
    override val refundEntry = "திரும்பப்பெறல்"

    override val expenseTitle = "செலவுகள்"
    override val addExpense = "செலவு சேர்"
    override val expenseName = "செலவு பெயர்"
    override val amount = "தொகை"
    override val totalExpenses = "மொத்த செலவு"
    override val noExpenses = "செலவுகள் இல்லை"
    override val deleteExpense = "செலவு நீக்கு"

    override val reports = "அறிக்கைகள்"
    override val reportsDashboard = "வணிக அறிக்கைகள்"
    override val businessInsights = "உங்கள் வணிக நுண்ணறிவு"
    override val today = "இன்று"
    override val week = "வாரம்"
    override val month = "மாதம்"
    override val all = "அனைத்தும்"
    override val topProducts = "சிறந்த பொருட்கள்"
    override val dailySales = "தினசரி விற்பனை"
    override val grossProfit = "மொத்த லாபம்"
    override val netProfit = "நிகர லாபம்"
    override val insights = "நுண்ணறிவு"

    override val settings = "அமைப்புகள்"
    override val shopDetails = "கடை விவரங்கள்"
    override val shopName = "கடையின் பெயர்"
    override val upiPayment = "UPI கட்டணம்"
    override val upiId = "UPI ID"
    override val saveUpiId = "UPI ID சேமி"
    override val printer = "அச்சுப்பொறி"
    override val selectPrinter = "Bluetooth அச்சுப்பொறி தேர்ந்தெடு"
    override val cloudBackup = "கிளவுட் காப்பு"
    override val enableBackup = "தானியங்கி காப்பை இயக்கு"
    override val backupFrequency = "காப்பு அலவு"
    override val manualOnly = "கைமுறை மட்டும்"
    override val daily = "தினசரி"
    override val weekly = "வாரம்"
    override val backupNow = "இப்போது காப்பிடு"
    override val restore = "மீட்டெடு"
    override val restoreFromBackup = "கடைசி காப்பிலிருந்து மீட்டெடு"
    override val lastBackup = "கடைசி காப்பு"
    override val language = "மொழி"
    override val selectLanguage = "மொழி தேர்ந்தெடு"

    override val stockConsumption = "இருப்பு பயன்பாடு"
    override val consumptionReason = "காரணம் (எ.கா. உள் பயன்பாடு, சேதம்)"
    override val recordConsumption = "பதிவு செய்"
    override val consumptionConfirm = "இருப்பை குறைத்து செலவாக பதிவு செய்யவா?"

    override val useCurrentLocation = "தற்போதைய இடம் பயன்படுத்து"
    override val pickOnMap = "வரைபடத்தில் தேர்"
    override val selectLocation = "இடம் தேர்"
    override val locationSelected = "இடம் தேர்ந்தெடுக்கப்பட்டது"

    override val recordPayment = "கட்டணம் பதிவு செய்"
    override val paymentAmount = "கட்டண தொகை (₹)"
    override val note = "குறிப்பு (விருப்பம்)"
    override val outstandingBalance = "நிலுவை தொகை"
    override val noBalance = "நிலுவை இல்லை"
    override val paymentRecorded = "கட்டணம் பதிவு செய்யப்பட்டது"
    override val quickFill = "விரைவு நிரப்பு"

    override val selectPrinterTitle = "அச்சுப்பொறி தேர்"
    override val noPairedDevices = "இணைக்கப்பட்ட Bluetooth சாதனங்கள் இல்லை. முதலில் Android அமைப்புகளில் இணைக்கவும்."
    override val savePrinterSelection = "அச்சுப்பொறி தேர்வு சேமி"
    override val clearSelection = "தேர்வை அழி"
    override val printerSaved = "அச்சுப்பொறி சேமிக்கப்பட்டது"
    override val retry = "மீண்டும் முயற்சி"

    override val invoice = "வரி விலைப்பட்டியல்"
    override val customer = "வாடிக்கையாளர்"
    override val itemLabel = "பொருள்"
    override val qtyLabel = "அளவு"
    override val priceLabel = "விலை"
    override val gstLabel = "GST"
    override val totalLabel = "மொத்தம்"
    override val paidLabel = "செலுத்தியது"
    override val balanceLabel = "நிலுவை"
    override val thankYou = "நன்றி! மீண்டும் வாருங்கள்"
    override val date = "தேதி"
}