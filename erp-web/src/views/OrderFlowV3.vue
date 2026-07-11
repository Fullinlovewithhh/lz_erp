<template>
  <div class="flow-page">
    <div class="flow-tabs">
      <button v-for="tab in tabs" :key="tab" :class="{ active: activeTab === tab }" @click="openTab(tab)">{{ tab }}</button>
    </div>

    <section v-if="activeTab === '客户订单'" class="flow-section">
      <div class="section-head"><h2>客户订单</h2><button class="primary" @click="createOrder">新建</button></div>
      <div class="form-row">
        <label>客户<select v-model="orderForm.customerId"><option value="">请选择</option><option v-for="c in customers" :key="c.id" :value="c.id">{{ c.customerName }}</option></select></label>
        <label>客户订单号<input v-model.trim="orderForm.customerOrderNo" /></label>
        <label>订单名称<input v-model.trim="orderForm.customerOrderName" placeholder="住宅1" /></label>
        <label>备注<input v-model.trim="orderForm.remark" /></label>
      </div>
      <div class="table-wrap"><table><thead><tr><th>客户</th><th>客户订单号</th><th>订单名称</th><th>CAD评审</th><th>报价</th><th>付款</th><th>下料放行</th><th>CAD</th></tr></thead>
        <tbody><tr v-for="o in orderRows" :key="o.id"><td>{{ customerName(o.customerId) }}</td><td>{{ o.customerOrderNo }}</td><td>{{ o.customerOrderName || '-' }}</td><td>{{ o.reviewStatus }}</td><td>{{ o.quoteStatus }}</td><td>{{ o.paymentStatus }}</td><td>{{ o.cuttingReleaseStatus }}</td><td><label class="file-action">上传<input type="file" @change="uploadCad(o.id, $event)" /></label></td></tr></tbody>
      </table></div>
    </section>

    <section v-if="activeTab === 'CAD评审'" class="flow-section">
      <div class="section-head"><h2>CAD评审池</h2><button @click="loadReviewPool">刷新</button></div>
      <div class="table-wrap"><table><thead><tr><th>客户</th><th>客户订单号</th><th>订单名称</th><th>状态</th><th>评审设计师</th><th>操作</th></tr></thead>
        <tbody><tr v-for="o in reviewPool" :key="o.id"><td>{{ o.customer_name }}</td><td>{{ o.customer_order_no }}</td><td>{{ o.customer_order_name || '-' }}</td><td>{{ o.review_status }}</td><td>{{ o.review_engineer_name || '-' }}</td><td><button v-if="!o.review_engineer_id" class="primary" @click="claimReview(o.id)">领取</button><button v-else @click="selectForSplit(o)">拆单草稿</button></td></tr></tbody>
      </table></div>
    </section>

    <section v-if="activeTab === '拆单确认'" class="flow-section">
      <div class="section-head"><h2>拆单草稿</h2><select v-model="selectedCustomerOrderId" @change="loadDrafts"><option value="">选择客户订单</option><option v-for="o in orderRows" :key="o.id" :value="o.id">{{ o.customerOrderNo }} / {{ o.customerOrderName }}</option></select></div>
      <div class="form-row">
        <label>工厂订单名称<input v-model.trim="draftForm.factoryOrderName" placeholder="住宅1-客厅" /></label>
        <label>生产线<select v-model="draftForm.productionLineId"><option value="">请选择</option><option v-for="l in enabledLines" :key="l.id" :value="l.id">{{ l.line_code }} / {{ l.line_name }}</option></select></label>
        <label>订单类型<select v-model="draftForm.orderType"><option value="NORMAL">正常订单</option><option value="SUPPLEMENT">补单</option></select></label>
        <label v-if="draftForm.orderType === 'SUPPLEMENT'">原工厂订单<select v-model="draftForm.parentFactoryOrderId"><option value="">请选择</option><option v-for="o in factoryOrders" :key="o.factory_order_id" :value="o.factory_order_id">{{ o.factory_order_id }} / {{ o.factory_order_name }}</option></select></label>
        <label>备注<input v-model.trim="draftForm.remark" /></label>
        <button class="primary align-end" @click="saveDraft">加入草稿</button>
      </div>
      <div class="table-wrap"><table><thead><tr><th>名称</th><th>生产线</th><th>类型</th><th>原订单</th><th>备注</th><th>状态</th></tr></thead><tbody><tr v-for="d in drafts" :key="d.id"><td>{{ d.factory_order_name }}</td><td>{{ d.line_code }}</td><td>{{ d.order_type === 'SUPPLEMENT' ? '补单' : '正常' }}</td><td>{{ d.parent_factory_order_id || '-' }}</td><td>{{ d.remark || '-' }}</td><td>{{ d.status }}</td></tr></tbody></table></div>
      <div class="action-bar"><button class="primary" :disabled="!drafts.some(d => d.status === '草稿')" @click="confirmSplit">客服确认并生成工厂订单</button></div>
    </section>

    <section v-if="activeTab === '工厂订单'" class="flow-section">
      <div class="section-head"><h2>工厂订单与报价负责人</h2><button @click="loadFactoryOrders">刷新</button></div>
      <div class="table-wrap"><table><thead><tr><th>工厂订单号</th><th>工厂订单名称</th><th>客户订单号</th><th>生产线</th><th>类型</th><th>报价负责人</th><th>状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="o in factoryOrders" :key="o.factory_order_id"><td>{{ o.factory_order_id }}</td><td>{{ o.factory_order_name }}</td><td>{{ o.customer_order_no }}</td><td>{{ o.line_code }}</td><td>{{ o.order_type === 'SUPPLEMENT' ? '补单' : '正常' }}</td><td>{{ o.quote_assignee_name || '订单池' }}</td><td>{{ o.status }}</td><td><button v-if="!o.quote_assignee_id" class="primary" @click="claimFactory(o.factory_order_id)">领取报价</button><button @click="assignFactory(o)">指定/转交</button><button @click="openQuote(o)">报价</button></td></tr></tbody>
      </table></div>
      <div v-if="quoteFactory" class="commercial-panel">
        <h3>{{ quoteFactory.factory_order_id }} / {{ quoteFactory.factory_order_name }}</h3>
        <div class="form-row"><label>整单折扣<input v-model.number="quoteForm.discountRate" type="number" min="0" max="1" step="0.01" /></label><label>报价说明<input v-model.trim="quoteForm.quoteDesc" /></label><button class="align-end" @click="addQuoteItem">增加明细</button><button class="primary align-end" @click="submitQuote">提交新版本</button></div>
        <div class="table-wrap"><table><thead><tr><th>分类</th><th>产品/五金</th><th>规格</th><th>数量</th><th>单位</th><th>单价</th><th>参与折扣</th><th>金额</th><th></th></tr></thead><tbody><tr v-for="(item,index) in quoteForm.items" :key="index"><td><select v-model="item.productCategory" @change="onQuoteCategory(item)"><option value="CUSTOM">定制产品</option><option value="HARDWARE">五金配件</option><option value="OTHER">其他</option></select></td><td><input v-model.trim="item.productName" list="hardware-options" /></td><td><input v-model.trim="item.specification" /></td><td><input v-model.number="item.quantity" type="number" min="0" /></td><td><input v-model.trim="item.unit" /></td><td><input v-model.number="item.unitPrice" type="number" min="0" /></td><td><input v-model="item.discountEligible" type="checkbox" :disabled="item.productCategory === 'HARDWARE'" /></td><td>{{ money(item.quantity * item.unitPrice * (item.discountEligible ? quoteForm.discountRate : 1)) }}</td><td><button @click="quoteForm.items.splice(index,1)">删除</button></td></tr></tbody></table></div>
        <datalist id="hardware-options"><option v-for="h in hardware" :key="h.id" :value="h.hardware_name" /></datalist>
      </div>
    </section>

    <section v-if="activeTab === '商务财务'" class="flow-section">
      <div class="section-head"><h2>报价结算与放行</h2><button @click="loadCommercial">刷新</button></div>
      <div class="table-wrap"><table><thead><tr><th>客户</th><th>客户订单号</th><th>报价合计</th><th>价格调整</th><th>最终应收</th><th>已确认到账</th><th>付款状态</th><th>放行状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="o in commercialOrders" :key="o.id"><td>{{ o.customer_name }}</td><td>{{ o.customer_order_no }}</td><td>{{ money(o.quote_total_amount) }}</td><td>{{ money(o.price_adjustment_amount) }}</td><td>{{ money(o.final_receivable_amount) }}</td><td>{{ money(o.confirmed_received_amount) }}</td><td>{{ o.payment_status }}</td><td>{{ o.cutting_release_status }}</td><td><button @click="openCommercial(o)">办理</button></td></tr></tbody>
      </table></div>

      <div v-if="commercialDetail.order" class="commercial-panel">
        <h3>{{ commercialDetail.order.customer_order_no }} / {{ commercialDetail.order.customer_order_name }}</h3>
        <div class="form-row">
          <label>客户最终成交价<input v-model.number="adjustmentForm.finalAmount" type="number" min="0" step="0.01" /></label>
          <label>调整原因<input v-model.trim="adjustmentForm.reason" /></label>
          <button class="primary align-end" @click="submitAdjustment">提交厂长审批</button>
        </div>
        <div class="form-row">
          <label>确认方式<select v-model="confirmationForm.confirmationMethod"><option>微信</option><option>电话</option><option>邮件</option><option>现场</option><option>其他</option></select></label>
          <label>客户联系人<input v-model.trim="confirmationForm.customerContact" /></label>
          <label>确认备注<input v-model.trim="confirmationForm.confirmationRemark" /></label>
          <button class="primary align-end" @click="confirmQuote">记录客户确认</button>
        </div>
        <div class="form-row">
          <label>结算类型<select v-model="paymentPlanForm.settlementType"><option value="FULL">全款</option><option value="PARTIAL">分期/部分付款</option><option value="MONTHLY">月结</option><option value="FREE">免单</option></select></label>
          <label>本期金额<input v-model.number="paymentPlanForm.amount" type="number" min="0" /></label>
          <label>到期日<input v-model="paymentPlanForm.dueDate" type="date" /></label>
          <button class="primary align-end" @click="submitPaymentPlan">提交付款计划</button>
        </div>
        <div class="form-row">
          <label>到账金额<input v-model.number="receiptForm.actualAmount" type="number" min="0" /></label>
          <label>付款方式<select v-model="receiptForm.paymentMethod"><option>现金</option><option>对公账户</option><option>银行卡</option><option>免单</option><option>挂账</option></select></label>
          <label>收款账户<select v-model="receiptForm.receivingAccountId"><option value="">无</option><option v-for="a in enabledAccounts" :key="a.id" :value="a.id">{{ a.account_name }}</option></select></label>
          <label>付款人<input v-model.trim="receiptForm.payerName" /></label>
          <button class="primary align-end" @click="submitReceipt">提交财务确认</button>
        </div>
        <div class="form-row">
          <label>申请类型<select v-model="releaseForm.requestType"><option>正常下料</option><option>部分付款下料</option><option>免单下料</option><option>月结下料</option><option>补单挂账</option></select></label>
          <label class="check"><input v-model="releaseForm.bossVerbalApproved" type="checkbox" />老板已口头同意</label>
          <label>原因<input v-model.trim="releaseForm.requestReason" /></label>
          <button class="primary align-end" @click="requestRelease">申请下料放行</button>
        </div>
      </div>

      <div class="queue-grid">
        <div><h3>厂长待办</h3><div v-for="r in queues.priceAdjustments || []" :key="`pa${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} 价格调整 {{ money(r.after_amount) }}</span><button @click="approveAdjustment(r.id, true)">批准</button><button @click="approveAdjustment(r.id, false)">驳回</button></div><div v-for="r in queues.directorReleases || []" :key="`dr${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} 下料申请</span><button @click="approveRelease(r.id, true)">批准</button><button @click="approveRelease(r.id, false)">驳回</button></div></div>
        <div><h3>财务待办</h3><div v-for="r in queues.receipts || []" :key="`rc${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} 到账 {{ money(r.actual_amount) }}</span><button @click="confirmReceipt(r.id, true)">确认</button><button @click="confirmReceipt(r.id, false)">驳回</button></div><div v-for="r in queues.financeReleases || []" :key="`fr${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} 放行核实</span><button @click="financeRelease(r.id, true)">确认</button><button @click="financeRelease(r.id, false)">驳回</button></div></div>
        <div><h3>客服可放行</h3><div v-for="r in queues.serviceReleases || []" :key="`sr${r.id}`" class="queue-row"><span>{{ r.customer_order_no }}</span><button class="primary" @click="releaseCutting(r.id)">推送下料</button></div></div>
        <div><h3>付款提醒</h3><div v-for="r in queues.paymentReminders || []" :key="`pm${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} / {{ r.due_date }} / {{ money(r.planned_amount) }}</span></div></div>
      </div>
    </section>

    <section v-if="activeTab === '配置'" class="flow-section">
      <div class="config-band"><h2>生产线</h2><div class="form-row"><label>编码<input v-model.trim="lineForm.lineCode" /></label><label>名称<input v-model.trim="lineForm.lineName" /></label><button class="primary align-end" @click="addLine">新增</button></div><div class="tag-list"><span v-for="l in lines" :key="l.id">{{ l.line_code }} / {{ l.line_name }} / {{ Number(l.enabled) ? '启用' : '停用' }}</span></div></div>
      <div class="config-band"><h2>收款账户</h2><div class="form-row"><label>编码<input v-model.trim="accountForm.accountCode" /></label><label>名称<input v-model.trim="accountForm.accountName" /></label><label>方式<input v-model.trim="accountForm.paymentMethod" /></label><label>银行<input v-model.trim="accountForm.bankName" /></label><button class="primary align-end" @click="addAccount">新增</button></div><div class="tag-list"><span v-for="a in accounts" :key="a.id">{{ a.account_code }} / {{ a.account_name }} / {{ a.payment_method }}</span></div></div>
      <div class="config-band"><h2>五金库</h2><div class="form-row"><label>编码<input v-model.trim="hardwareForm.hardwareCode" /></label><label>名称<input v-model.trim="hardwareForm.hardwareName" /></label><label>单位<input v-model.trim="hardwareForm.unit" /></label><label>报价<input v-model.number="hardwareForm.salePrice" type="number" /></label><label>库存模式<select v-model="hardwareForm.stockMode"><option value="STOCK">常用库存</option><option value="PURCHASE">按单采购</option></select></label><button class="primary align-end" @click="addHardware">新增</button></div><div class="table-wrap"><table><thead><tr><th>编码</th><th>名称</th><th>模式</th><th>报价</th><th>现有库存</th><th>可用库存</th></tr></thead><tbody><tr v-for="h in hardware" :key="h.id"><td>{{ h.hardware_code }}</td><td>{{ h.hardware_name }}</td><td>{{ h.stock_mode }}</td><td>{{ money(h.sale_price) }}</td><td>{{ h.on_hand_quantity || 0 }}</td><td>{{ h.available_quantity || 0 }}</td></tr></tbody></table></div></div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import * as api from '../api/contract'

const tabs = ['客户订单', 'CAD评审', '拆单确认', '工厂订单', '商务财务', '配置']
const activeTab = ref('客户订单')
const customers = ref([])
const orderRows = ref([])
const reviewPool = ref([])
const lines = ref([])
const drafts = ref([])
const factoryOrders = ref([])
const commercialOrders = ref([])
const commercialDetail = ref({})
const queues = ref({})
const accounts = ref([])
const hardware = ref([])
const selectedCustomerOrderId = ref('')
const quoteFactory = ref(null)
const enabledLines = computed(() => lines.value.filter(l => Number(l.enabled) === 1))
const enabledAccounts = computed(() => accounts.value.filter(a => Number(a.enabled) === 1))

const orderForm = ref({ customerId: '', customerOrderNo: '', customerOrderName: '', remark: '' })
const draftForm = ref({ factoryOrderName: '', productionLineId: '', orderType: 'NORMAL', parentFactoryOrderId: '', remark: '' })
const adjustmentForm = ref({ finalAmount: 0, reason: '' })
const confirmationForm = ref({ confirmationMethod: '微信', customerContact: '', confirmationRemark: '' })
const paymentPlanForm = ref({ settlementType: 'FULL', amount: 0, dueDate: '' })
const receiptForm = ref({ actualAmount: 0, paymentMethod: '对公账户', receivingAccountId: '', payerName: '' })
const releaseForm = ref({ requestType: '正常下料', bossVerbalApproved: false, requestReason: '' })
const lineForm = ref({ lineCode: '', lineName: '' })
const accountForm = ref({ accountCode: '', accountName: '', paymentMethod: '', bankName: '' })
const hardwareForm = ref({ hardwareCode: '', hardwareName: '', unit: '个', salePrice: 0, stockMode: 'STOCK' })
const blankQuoteItem = () => ({ productCategory: 'CUSTOM', productName: '', specification: '', quantity: 1, unit: '件', unitPrice: 0, discountEligible: true, productId: null, hardwareItemId: null, remark: '' })
const quoteForm = ref({ discountRate: 1, quoteDesc: '', items: [blankQuoteItem()] })

onMounted(refreshAll)

async function refreshAll() {
  await Promise.all([loadCustomers(), loadOrders(), loadLines(), loadFactoryOrders(), loadCommercial(), loadAccounts(), loadHardware()])
}
async function openTab(tab) { activeTab.value = tab; if (tab === 'CAD评审') await loadReviewPool(); if (tab === '商务财务') await loadCommercial() }
async function loadCustomers() { customers.value = (await api.listCustomers('')).data.data || [] }
async function loadOrders() { orderRows.value = (await api.listCustomerOrders(null, null, '')).data.data || [] }
async function loadReviewPool() { reviewPool.value = (await api.listReviewPoolV3()).data.data || [] }
async function loadLines() { lines.value = (await api.listProductionLines()).data.data || [] }
async function loadFactoryOrders() { factoryOrders.value = (await api.listFactoryOrdersV3()).data.data || [] }
async function loadCommercial() { commercialOrders.value = (await api.listCommercialOrdersV3()).data.data || []; queues.value = (await api.getOrderFlowWorkQueues()).data.data || {} }
async function loadAccounts() { accounts.value = (await api.listReceivingAccountsV3()).data.data || [] }
async function loadHardware() { hardware.value = (await api.listHardwareItemsV3('')).data.data || [] }
function customerName(id) { return customers.value.find(c => String(c.id) === String(id))?.customerName || '-' }
function money(value) { return Number(value || 0).toFixed(2) }
function message(error) { window.alert(error?.response?.data?.message || error?.message || '操作失败') }

async function createOrder() { try { await api.createCustomerOrder({ ...orderForm.value, customerId: Number(orderForm.value.customerId), projectId: null }); orderForm.value = { customerId: '', customerOrderNo: '', customerOrderName: '', remark: '' }; await loadOrders() } catch (e) { message(e) } }
async function uploadCad(id, event) { const file = event.target.files?.[0]; if (!file) return; try { await api.uploadCustomerOrderCad(id, file); window.alert('CAD已上传') } catch (e) { message(e) } finally { event.target.value = '' } }
async function claimReview(id) { try { await api.claimCustomerOrderReview(id); await loadReviewPool(); await loadOrders() } catch (e) { message(e) } }
async function selectForSplit(order) { selectedCustomerOrderId.value = String(order.id); activeTab.value = '拆单确认'; await loadDrafts() }
async function loadDrafts() { if (!selectedCustomerOrderId.value) { drafts.value = []; return }; drafts.value = (await api.listSplitDrafts(Number(selectedCustomerOrderId.value))).data.data || [] }
async function saveDraft() { try { await api.addSplitDraft(Number(selectedCustomerOrderId.value), { ...draftForm.value, productionLineId: Number(draftForm.value.productionLineId), parentFactoryOrderId: draftForm.value.parentFactoryOrderId || null }); draftForm.value = { factoryOrderName: '', productionLineId: '', orderType: 'NORMAL', parentFactoryOrderId: '', remark: '' }; await loadDrafts() } catch (e) { message(e) } }
async function confirmSplit() { try { await api.confirmCustomerOrderSplit(Number(selectedCustomerOrderId.value)); await Promise.all([loadDrafts(), loadFactoryOrders(), loadOrders()]) } catch (e) { message(e) } }
async function claimFactory(id) { try { await api.claimFactoryOrderV3(id); await loadFactoryOrders() } catch (e) { message(e) } }
async function assignFactory(order) { const userId = window.prompt('请输入新的报价负责人用户ID'); if (!userId) return; const reason = window.prompt('填写指定或转交原因') || ''; try { await api.assignFactoryOrderV3(order.factory_order_id, { userId: Number(userId), reason }); await loadFactoryOrders() } catch (e) { message(e) } }
function openQuote(order) { quoteFactory.value = order; quoteForm.value = { discountRate: Number(order.discount_rate || 1), quoteDesc: '', items: [blankQuoteItem()] } }
function addQuoteItem() { quoteForm.value.items.push(blankQuoteItem()) }
function onQuoteCategory(item) { if (item.productCategory === 'HARDWARE') item.discountEligible = false }
async function submitQuote() { try { await api.saveFactoryOrderQuoteV3(quoteFactory.value.factory_order_id, { ...quoteForm.value, submit: true }); quoteFactory.value = null; await Promise.all([loadFactoryOrders(), loadCommercial()]) } catch (e) { message(e) } }
async function openCommercial(order) { commercialDetail.value = (await api.getCommercialDetailV3(order.id)).data.data || {}; adjustmentForm.value.finalAmount = Number(order.final_receivable_amount || order.quote_total_amount || 0) }
async function submitAdjustment() { try { await api.requestPriceAdjustmentV3(commercialDetail.value.order.id, adjustmentForm.value); await loadCommercial() } catch (e) { message(e) } }
async function approveAdjustment(id, approved) { try { await api.approvePriceAdjustmentV3(id, { approved, remark: '' }); await loadCommercial() } catch (e) { message(e) } }
async function confirmQuote() { try { await api.confirmCustomerQuoteV3(commercialDetail.value.order.id, { ...confirmationForm.value, confirmedAt: new Date().toISOString(), attachmentPath: null }); await loadCommercial() } catch (e) { message(e) } }
async function submitPaymentPlan() { try { await api.createPaymentPlanV3(commercialDetail.value.order.id, { settlementType: paymentPlanForm.value.settlementType, customerConfirmed: true, installments: [{ installmentNo: 1, plannedAmount: Number(paymentPlanForm.value.amount), dueDate: paymentPlanForm.value.dueDate }] }); await loadCommercial() } catch (e) { message(e) } }
async function submitReceipt() { try { await api.submitPaymentReceiptV3(commercialDetail.value.order.id, { ...receiptForm.value, receivingAccountId: receiptForm.value.receivingAccountId ? Number(receiptForm.value.receivingAccountId) : null, receivedAt: new Date().toISOString(), paymentPlanId: null, bankReference: '', voucherPath: null }); await loadCommercial() } catch (e) { message(e) } }
async function confirmReceipt(id, approved) { try { await api.confirmPaymentReceiptV3(id, { approved, remark: '' }); await loadCommercial() } catch (e) { message(e) } }
async function requestRelease() { try { await api.requestCuttingReleaseV3(commercialDetail.value.order.id, { ...releaseForm.value, bossConfirmedAt: releaseForm.value.bossVerbalApproved ? new Date().toISOString() : null }); await loadCommercial() } catch (e) { message(e) } }
async function financeRelease(id, approved) { try { await api.financeConfirmCuttingReleaseV3(id, { approved, remark: '' }); await loadCommercial() } catch (e) { message(e) } }
async function approveRelease(id, approved) { try { await api.directorApproveCuttingReleaseV3(id, { approved, remark: '' }); await loadCommercial() } catch (e) { message(e) } }
async function releaseCutting(id) { try { await api.releaseToCuttingV3(id); await Promise.all([loadCommercial(), loadFactoryOrders()]) } catch (e) { message(e) } }
async function addLine() { try { await api.createProductionLineV3({ ...lineForm.value, enabled: true, sortOrder: lines.value.length * 10 + 10 }); lineForm.value = { lineCode: '', lineName: '' }; await loadLines() } catch (e) { message(e) } }
async function addAccount() { try { await api.createReceivingAccountV3({ ...accountForm.value, enabled: true, sortOrder: accounts.value.length * 10 + 10, accountNoMasked: '' }); accountForm.value = { accountCode: '', accountName: '', paymentMethod: '', bankName: '' }; await loadAccounts() } catch (e) { message(e) } }
async function addHardware() { try { await api.createHardwareItemV3({ ...hardwareForm.value, enabled: true, specification: '' }); hardwareForm.value = { hardwareCode: '', hardwareName: '', unit: '个', salePrice: 0, stockMode: 'STOCK' }; await loadHardware() } catch (e) { message(e) } }
</script>

<style scoped>
.flow-page{display:grid;gap:14px}.flow-tabs{display:flex;gap:4px;border-bottom:1px solid #d7dce2}.flow-tabs button{border:0;background:transparent;padding:10px 14px}.flow-tabs button.active{color:#116149;border-bottom:2px solid #116149;font-weight:700}.flow-section{display:grid;gap:14px}.section-head{display:flex;align-items:center;justify-content:space-between}.section-head h2,.config-band h2{font-size:17px;margin:0}.form-row{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:10px;align-items:end}.form-row label{display:grid;gap:5px;font-size:12px;color:#4d5965}.form-row input,.form-row select,.section-head select{width:100%;height:36px;border:1px solid #cfd6dd;padding:0 9px;background:#fff}.form-row button,.section-head button,.table-wrap button,.queue-row button,.action-bar button{height:34px;border:1px solid #c8d0d8;background:#fff;padding:0 11px}.primary{background:#116149!important;border-color:#116149!important;color:#fff}.align-end{align-self:end}.table-wrap{overflow:auto;border-top:1px solid #d9dee3}.table-wrap table{width:100%;border-collapse:collapse;white-space:nowrap}.table-wrap th,.table-wrap td{padding:9px 10px;border-bottom:1px solid #e4e8ec;text-align:left;font-size:12px}.table-wrap th{background:#f5f7f8;color:#52606c}.file-action{cursor:pointer;color:#116149}.file-action input{display:none}.action-bar{display:flex;justify-content:flex-end}.commercial-panel{border-top:1px solid #d9dee3;border-bottom:1px solid #d9dee3;padding:14px 0;display:grid;gap:12px}.commercial-panel h3{margin:0;font-size:15px}.check{display:flex!important;grid-template-columns:18px 1fr!important;align-items:center;height:36px}.check input{width:16px;height:16px}.queue-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}.queue-grid>div{border-top:2px solid #87939d;padding-top:8px}.queue-grid h3{font-size:14px;margin:0 0 8px}.queue-row{display:flex;align-items:center;gap:6px;padding:7px 0;border-bottom:1px solid #e6eaed;font-size:12px}.queue-row span{flex:1}.config-band{display:grid;gap:10px;padding-bottom:14px;border-bottom:1px solid #d9dee3}.tag-list{display:flex;flex-wrap:wrap;gap:6px}.tag-list span{border:1px solid #d5dbe0;padding:5px 8px;font-size:12px}@media(max-width:700px){.flow-tabs{overflow:auto}.form-row{grid-template-columns:1fr}.queue-grid{grid-template-columns:1fr}}
</style>
