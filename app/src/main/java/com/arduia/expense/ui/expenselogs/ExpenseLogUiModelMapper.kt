package com.arduia.expense.ui.expenselogs

import com.arduia.core.arch.Mapper
import com.arduia.expense.data.local.ExpenseEnt
import com.arduia.expense.di.CurrencyDecimalFormat
import com.arduia.expense.ui.common.category.ExpenseCategory
import com.arduia.expense.ui.common.category.ExpenseCategoryProvider
import com.arduia.expense.ui.common.category.ExpenseCategoryProviderImpl
import com.arduia.expense.ui.common.formatter.DateFormatter
import com.arduia.expense.ui.home.CurrencyProvider
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExpenseLogUiModelMapper  @Inject constructor(
    private val categoryProvider: ExpenseCategoryProvider,
    private val dateFormatter: DateFormatter,
    @CurrencyDecimalFormat private val currencyFormatter: NumberFormat,
    private val provider: CurrencyProvider = CurrencyProvider{""}
) : Mapper<ExpenseEnt, ExpenseLogUiModel.Log> {

    private val headerKeyFormatter = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
    private val headerLabelFormatter = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    private val headerLabelWithYearFormatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    override fun map(input: ExpenseEnt): ExpenseLogUiModel.Log {
        val categoryDrawable = try {
            categoryProvider.getCategoryDrawableByID(input.category)
        } catch (e: Exception) {
            categoryProvider.getCategoryDrawableByID(ExpenseCategory.OTHERS)
        }

        return ExpenseLogUiModel.Log(
            ExpenseUiModel(
                id = input.expenseId,
                name = input.name ?: "",
                date = dateFormatter.format(input.modifiedDate),
                amount = currencyFormatter.format(
                    BigDecimal.valueOf(
                        input.amount.getActual().toDouble()
                    )
                ),
                finance = "",
                category = categoryDrawable,
                currencySymbol = provider.get()
            ),
            headerKey = formatHeaderKey(input.modifiedDate),
            headerLabel = formatHeaderLabel(input.modifiedDate)
        )
    }

    @Synchronized
    private fun formatHeaderKey(time: Long): String {
        return headerKeyFormatter.format(Date(time))
    }

    @Synchronized
    private fun formatHeaderLabel(time: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = time }
        val formatter = if (target[Calendar.YEAR] == now[Calendar.YEAR]) {
            headerLabelFormatter
        } else {
            headerLabelWithYearFormatter
        }
        return formatter.format(Date(time))
    }

}

class ExpenseUiModelMapperFactoryImpl @Inject constructor(
    private val categoryProvider: ExpenseCategoryProviderImpl,
    private val dateFormatter: DateFormatter,
    @CurrencyDecimalFormat private val currencyFormatter: NumberFormat
) : ExpenseUiModelMapperFactory {
    override fun create(provider: CurrencyProvider): Mapper<ExpenseEnt, ExpenseLogUiModel.Log> {
        return ExpenseLogUiModelMapper(
            categoryProvider,
            dateFormatter,
            currencyFormatter,
            provider
        )
    }
}

interface ExpenseUiModelMapperFactory : Mapper.Factory<ExpenseEnt, ExpenseLogUiModel.Log> {
    fun create(provider: CurrencyProvider): Mapper<ExpenseEnt, ExpenseLogUiModel.Log>
}
