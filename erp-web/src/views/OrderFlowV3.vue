<template>
  <div class="flow-page">
    <section v-if="activeTab === '客户订单'" class="flow-section">
      <div class="section-head"><h2>客户订单</h2><button class="primary" @click="createOrder">新建</button></div>
      <div class="form-row">
        <label>客户<select v-model="orderForm.customerId"><option value="">请选择</option><option v-for="c in customers" :key="c.id" :value="c.id">{{ c.customerName }}</option></select></label>
        <label>客户订单号<input v-model.trim="orderForm.customerOrderNo" /></label>
        <label class="file-action">客户CAD及附件<input type="file" multiple @change="selectOrderFiles" /><span>{{ orderFiles.length ? `已选 ${orderFiles.length} 个文件` : '选择文件' }}</span></label>
        <label>备注<input v-model.trim="orderForm.remark" /></label>
      </div>
      <div class="table-wrap"><table><thead><tr><th>客户</th><th>客户订单号</th><th>CAD评审</th><th>报价</th><th>付款</th><th>下料放行</th><th>CAD</th></tr></thead>
        <tbody><tr v-for="o in orderRows" :key="o.id"><td>{{ customerName(o.customerId) }}</td><td>{{ o.customerOrderNo }}</td><td>{{ o.reviewStatus }}</td><td>{{ o.quoteStatus }}</td><td>{{ o.paymentStatus }}</td><td>{{ o.cuttingReleaseStatus }}</td><td><label class="file-action">上传<input type="file" @change="uploadCad(o.id, $event)" /></label></td></tr></tbody>
      </table></div>
    </section>

    <section v-if="activeTab === 'CAD评审'" class="flow-section">
      <div class="section-head"><h2>CAD评审池</h2><button @click="loadReviewPool">刷新</button></div>
      <div class="table-wrap"><table><thead><tr><th>客户</th><th>客户订单号</th><th>状态</th><th>评审设计师</th><th>操作</th></tr></thead>
        <tbody><tr v-for="o in reviewPool" :key="o.id"><td>{{ o.customer_name }}</td><td>{{ o.customer_order_no }}</td><td>{{ o.review_status }}</td><td>{{ o.review_engineer_name || '-' }}</td><td><button v-if="!o.review_engineer_id" class="primary" @click="claimReview(o.id)">领取</button><button v-else @click="selectForSplit(o)">拆单草稿</button></td></tr></tbody>
      </table></div>
    </section>

    <section v-if="activeTab === '拆单'" class="flow-section">
      <div class="section-head"><h2>拆单草稿</h2><select v-model="selectedCustomerOrderId" @change="loadDrafts"><option value="">选择客户订单</option><option v-for="o in orderRows" :key="o.id" :value="o.id">{{ o.customerOrderNo }}</option></select></div>
      <div class="form-row">
        <label>工厂订单名称<input v-model.trim="draftForm.factoryOrderName" placeholder="住宅1-客厅" /></label>
        <label>生产线<select v-model="draftForm.productionLineId"><option value="">请选择</option><option v-for="l in enabledLines" :key="l.id" :value="l.id">{{ l.line_code }} / {{ l.line_name }}</option></select></label>
        <label>订单类型<select v-model="draftForm.orderType"><option value="NORMAL">正常订单</option><option value="SUPPLEMENT">补单</option></select></label>
        <label v-if="draftForm.orderType === 'SUPPLEMENT'">原工厂订单<select v-model="draftForm.parentFactoryOrderId"><option value="">请选择</option><option v-for="o in factoryOrders" :key="o.factory_order_id" :value="o.factory_order_id">{{ o.factory_order_id }} / {{ o.factory_order_name }}</option></select></label>
        <label>备注<input v-model.trim="draftForm.remark" /></label>
        <button class="primary align-end" @click="saveDraft">加入草稿</button>
      </div>
      <div class="table-wrap"><table><thead><tr><th>名称</th><th>生产线</th><th>类型</th><th>原订单</th><th>备注</th><th>状态</th></tr></thead><tbody><tr v-for="d in drafts" :key="d.id"><td>{{ d.factory_order_name }}</td><td>{{ d.line_code }}</td><td>{{ d.order_type === 'SUPPLEMENT' ? '补单' : '正常' }}</td><td>{{ d.parent_factory_order_id || '-' }}</td><td>{{ d.remark || '-' }}</td><td>{{ d.status }}</td></tr></tbody></table></div>
      <div class="action-bar"><button class="primary" :disabled="!drafts.some(d => d.status === '草稿')" @click="submitSplit">提交拆单并进入报价池</button></div>
    </section>

    <section v-if="activeTab === '工厂订单'" class="flow-section">
      <div class="section-head"><h2>工厂订单与报价负责人</h2><button @click="loadFactoryOrders">刷新</button></div>
      <div class="table-wrap"><table><thead><tr><th>工厂订单号</th><th>工厂订单名称</th><th>客户订单号</th><th>生产线</th><th>报价方式</th><th>类型</th><th>报价负责人</th><th>状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="o in factoryOrders" :key="o.factory_order_id"><td>{{ o.factory_order_id }}</td><td>{{ o.factory_order_name }}</td><td>{{ o.customer_order_no }}</td><td>{{ o.line_name }}</td><td>{{ o.quote_mode === 'EXTERNAL' ? '外部系统报价' : 'ERP内部报价' }}</td><td>{{ o.order_type === 'SUPPLEMENT' ? '补单' : '正常' }}</td><td>{{ o.quote_mode === 'EXTERNAL' ? '-' : (o.quote_assignee_name || '订单池') }}</td><td>{{ o.status }}</td><td><template v-if="o.quote_mode !== 'EXTERNAL'"><button v-if="!o.quote_assignee_id" class="primary" @click="claimFactory(o.factory_order_id)">领取报价</button><button @click="assignFactory(o)">指定/转交</button><button @click="openQuote(o)">报价</button></template><button v-else class="primary" @click="openExternalQuote(o)">登记外部报价</button></td></tr></tbody>
      </table></div>
      <div v-if="externalQuoteFactory" class="commercial-panel">
        <div class="section-head"><h3>{{ externalQuoteFactory.factory_order_id }} / {{ externalQuoteFactory.factory_order_name }} / 板式外部报价</h3><button @click="externalQuoteFactory=null">关闭</button></div>
        <div class="form-row">
          <label>外部报价单号<input v-model.trim="externalQuoteForm.externalQuoteNo" /></label>
          <label>最终报价金额<input v-model.number="externalQuoteForm.finalAmount" type="number" min="0" step="0.01" /></label>
          <label>报价日期<input v-model="externalQuoteForm.quoteDate" type="date" /></label>
          <label class="file-action">报价附件<input type="file" @change="uploadExternalQuoteFile" /><span>{{ externalQuoteForm.attachmentPath ? '已上传' : '选择文件' }}</span></label>
          <label class="check"><input v-model="externalQuoteForm.customerConfirmed" type="checkbox" />客户已确认</label>
          <label>备注<input v-model.trim="externalQuoteForm.remark" /></label>
          <button class="primary align-end" @click="saveExternalQuote">保存外部报价版本</button>
        </div>
        <div class="tag-list"><span v-for="q in externalQuoteVersions" :key="q.id">V{{ q.version_no }} / {{ money(q.final_amount) }} / {{ q.confirmation_status }} / {{ q.quote_date }}</span></div>
      </div>
      <div v-if="quoteFactory" class="commercial-panel">
        <div class="section-head"><h3>{{ quoteFactory.factory_order_id }} / {{ quoteFactory.factory_order_name }}</h3><div><button @click="addQuoteItem">增加明细</button><button @click="submitQuote(false)">暂存</button><button class="primary" @click="submitQuote(true)">提交新版本</button></div></div>
        <div class="form-row"><label>工厂订单折扣<input v-model.number="quoteForm.discountRate" type="number" min="0" max="1" step="0.01" /></label><label>客户标准折扣<input :value="quoteFactory.customer_discount_rate || 1" readonly /></label><label>临时折扣原因<input v-model.trim="quoteForm.discountReason" /></label><button class="align-end" @click="requestDiscount">申请临时折扣</button><label>报价说明<input v-model.trim="quoteForm.quoteDesc" /></label></div>
        <div v-for="(item,index) in quoteForm.items" :key="index" class="quote-item-editor">
          <div class="quote-item-head"><strong>明细 {{ index + 1 }}</strong><button @click="quoteForm.items.splice(index,1)">删除</button></div>
          <div class="quote-fields">
            <label>明细来源<select v-model="item.sourceType" @change="onQuoteSource(item)"><option value="PRODUCT">产品资料</option><option value="HARDWARE">五金库</option></select></label>
            <label v-if="item.sourceType === 'PRODUCT'">产品<select v-model="item.productId" @change="selectCatalogItem(item)"><option value="">请选择</option><option v-for="p in products" :key="p.id" :value="p.id">{{ p.type }} / {{ p.model || p.product_name }}</option></select></label>
            <label v-else>五金<select v-model="item.hardwareItemId" @change="selectCatalogItem(item)"><option value="">请选择</option><option v-for="h in hardware" :key="h.id" :value="h.id">{{ h.hardware_name }}</option></select></label>
            <label>产品类别<input :value="item.productCategory || '-'" readonly /></label>
            <label>材质结构<input v-model.trim="item.materialStructure" list="material-options" /></label><label>拉手颜色<input v-model.trim="item.handleColor" /></label>
            <label v-if="item.pricingMode === 'AREA'">宽(mm)<input v-model.number="item.widthMm" type="number" min="0" /></label><label v-if="item.pricingMode === 'AREA'">高(mm)<input v-model.number="item.heightMm" type="number" min="0" /></label><label v-if="item.pricingMode === 'LENGTH'">长度(mm)<input v-model.number="item.lengthMm" type="number" min="0" /></label><label>厚(mm)<input v-model.number="item.thicknessMm" type="number" min="0" /></label>
            <label>数量<input v-model.number="item.quantity" type="number" min="1" /></label><label>计价方式<input :value="pricingModeText(item.pricingMode)" readonly /></label><label>基础单价<input :value="`${money(item.baseUnitPrice)} / ${item.unit || '-'}`" readonly /></label>
            <label>铰链孔<input v-model.trim="item.hingeHole" /></label><label>工艺说明<input v-model.trim="item.processDesc" /></label><label>生产工序<input v-model.trim="item.productionProcess" /></label><label>技术员<input v-model.trim="item.technician" /></label>
            <label class="check"><input v-model="item.discountEligible" type="checkbox" disabled />产品参与折扣</label>
            <label class="file-action">明细图片<input type="file" accept="image/*" @change="uploadQuoteImage(item,$event)" /></label><span>{{ item.attachmentName || '未上传' }}</span>
          </div>
          <div v-if="item.sourceType !== 'HARDWARE'" class="rule-area">
            <strong>特殊工艺加价</strong>
            <div class="rule-list"><label v-for="r in quoteRules" :key="r.id"><input type="checkbox" :checked="item.selectedRuleIds.includes(r.id)" @change="toggleRule(item,r,$event)" />{{ r.rule_name }} / {{ r.adjust_value }}{{ r.unit_desc }} <input type="checkbox" :checked="!item.nonDiscountRuleIds.includes(r.id)" :disabled="!item.selectedRuleIds.includes(r.id)" @change="toggleRuleDiscount(item,r,$event)" />参与折扣</label></div>
            <div class="custom-rule-row"><input v-model.trim="item.newRule.ruleCategory" placeholder="加价分类" /><input v-model.trim="item.newRule.ruleName" placeholder="一次性加价名称" /><input v-model.trim="item.newRule.chargeReason" placeholder="加价原因，如专用刀具" /><select v-model="item.newRule.adjustMode"><option value="FIXED_PER_M2">每平方米加价</option><option value="FIXED_PER_METER">每米加价</option><option value="FIXED_PER_ITEM">每件加价</option><option value="FIXED_ONCE">本明细固定加价</option><option value="PERCENT">按基础金额百分比</option></select><input v-model.number="item.newRule.adjustValue" type="number" placeholder="加价值" /><label class="check"><input v-model="item.newRule.discountEligible" type="checkbox" />参与折扣</label><button @click="addCustomRule(item)">加入</button></div>
            <div class="tag-list"><span v-for="(r,ri) in item.customRules" :key="ri">{{ r.ruleName }} {{ r.adjustValue }}{{ r.unitDesc || '' }} / {{ item.nonDiscountCustomRuleNames.includes(r.ruleName) ? '不折扣' : '参与折扣' }} <button @click="removeCustomRule(item,ri)">×</button></span></div>
          </div>
        </div>
        <datalist id="material-options"><option v-for="m in materials" :key="m.id" :value="m.material_name" /></datalist>
      </div>
    </section>

    <section v-if="activeTab === '商务财务'" class="flow-section">
      <div class="section-head"><h2>报价结算与放行</h2><button @click="loadCommercial">刷新</button></div>
      <div class="table-wrap"><table><thead><tr><th>客户</th><th>客户订单号</th><th>报价合计</th><th>价格调整</th><th>最终应收</th><th>已确认到账</th><th>付款状态</th><th>放行状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="o in commercialOrders" :key="o.id"><td>{{ o.customer_name }}</td><td>{{ o.customer_order_no }}</td><td>{{ money(o.quote_total_amount) }}</td><td>{{ money(o.price_adjustment_amount) }}</td><td>{{ money(o.final_receivable_amount) }}</td><td>{{ money(o.confirmed_received_amount) }}</td><td>{{ o.payment_status }}</td><td>{{ o.cutting_release_status }}</td><td><button @click="openCommercial(o)">办理</button></td></tr></tbody>
      </table></div>

      <div v-if="commercialDetail.order" class="commercial-panel">
        <h3>{{ commercialDetail.order.customer_order_no }}</h3>
        <div class="form-row">
          <label>客户最终成交价<input v-model.number="adjustmentForm.finalAmount" type="number" min="0" step="0.01" /></label>
          <label>调整原因<input v-model.trim="adjustmentForm.reason" /></label>
          <button class="primary align-end" @click="submitAdjustment">提交厂长审批</button>
        </div>
        <div class="form-row">
          <label>税率<input v-model.number="pdfForm.taxRate" type="number" min="0" max="1" step="0.01" /></label>
          <label>报价有效期（天）<input v-model.number="pdfForm.validDays" type="number" min="1" /></label>
          <label>PDF备注<input v-model.trim="pdfForm.quoteRemark" /></label>
          <button class="primary align-end" @click="generatePdf">生成客户报价PDF</button>
        </div>
        <div class="tag-list"><span v-for="p in quotePdfs" :key="p.id">V{{ p.pdf_version }} / {{ p.status }} / 价税合计 {{ money(p.tax_included_total) }} <button @click="downloadPdf(p)">下载</button></span></div>
        <div class="form-row">
          <label>客户确认PDF版本<select v-model="confirmationForm.pdfId"><option value="">请选择</option><option v-for="p in quotePdfs.filter(x=>x.status!=='已失效')" :key="p.id" :value="p.id">V{{ p.pdf_version }} / {{ p.status }}</option></select></label>
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
        <div><h3>厂长待办</h3><div v-for="r in queues.discountRequests || []" :key="`ds${r.id}`" class="queue-row"><span>{{ r.factory_order_id }} 临时折扣 {{ r.requested_rate }}</span><button @click="approveDiscount(r.id, true)">批准</button><button @click="approveDiscount(r.id, false)">驳回</button></div><div v-for="r in queues.priceAdjustments || []" :key="`pa${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} 价格调整 {{ money(r.after_amount) }}</span><button @click="approveAdjustment(r.id, true)">批准</button><button @click="approveAdjustment(r.id, false)">驳回</button></div><div v-for="r in queues.directorReleases || []" :key="`dr${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} 下料申请</span><button @click="approveRelease(r.id, true)">批准</button><button @click="approveRelease(r.id, false)">驳回</button></div></div>
        <div><h3>财务待办</h3><div v-for="r in queues.receipts || []" :key="`rc${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} 到账 {{ money(r.actual_amount) }}</span><button @click="confirmReceipt(r.id, true)">确认</button><button @click="confirmReceipt(r.id, false)">驳回</button></div><div v-for="r in queues.financeReleases || []" :key="`fr${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} 放行核实</span><button @click="financeRelease(r.id, true)">确认</button><button @click="financeRelease(r.id, false)">驳回</button></div></div>
        <div><h3>客服可放行</h3><div v-for="r in queues.serviceReleases || []" :key="`sr${r.id}`" class="queue-row"><span>{{ r.customer_order_no }}</span><button class="primary" @click="releaseCutting(r.id)">推送下料</button></div></div>
        <div><h3>付款提醒</h3><div v-for="r in queues.paymentReminders || []" :key="`pm${r.id}`" class="queue-row"><span>{{ r.customer_order_no }} / {{ r.due_date }} / {{ money(r.planned_amount) }}</span></div></div>
      </div>
    </section>

    <section v-if="activeTab === '配置'" class="flow-section">
      <div class="config-band"><h2>公司报价资料</h2><div class="form-row"><label>公司名称<input v-model.trim="companyProfile.companyName" /></label><label>公司地址<input v-model.trim="companyProfile.companyAddress" /></label><label>联系电话<input v-model.trim="companyProfile.contactPhone" /></label><label class="file-action">公司Logo<input type="file" accept="image/*" @change="uploadCompanyLogo" /></label><span>{{ companyProfile.logoPath || '未上传' }}</span><button class="primary align-end" @click="saveCompanyProfile">保存</button></div></div>
      <div class="config-band"><h2>生产线</h2><div class="form-row"><label>编码<input v-model.trim="lineForm.lineCode" /></label><label>名称<input v-model.trim="lineForm.lineName" /></label><label>报价方式<select v-model="lineForm.quoteMode"><option value="ERP">ERP内部报价</option><option value="EXTERNAL">外部系统报价</option><option value="NONE">不参与报价</option></select></label><button class="primary align-end" @click="addLine">新增</button></div><div class="tag-list"><span v-for="l in lines" :key="l.id">{{ l.line_code }} / {{ l.line_name }} / {{ l.quote_mode === 'EXTERNAL' ? '外部报价' : (l.quote_mode === 'NONE' ? '不报价' : '内部报价') }} / {{ Number(l.enabled) ? '启用' : '停用' }}</span></div></div>
      <div class="config-band"><h2>收款账户</h2><div class="form-row"><label>编码<input v-model.trim="accountForm.accountCode" /></label><label>名称<input v-model.trim="accountForm.accountName" /></label><label>方式<input v-model.trim="accountForm.paymentMethod" /></label><label>银行<input v-model.trim="accountForm.bankName" /></label><button class="primary align-end" @click="addAccount">新增</button></div><div class="tag-list"><span v-for="a in accounts" :key="a.id">{{ a.account_code }} / {{ a.account_name }} / {{ a.payment_method }}</span></div></div>
      <div class="config-band"><h2>五金库</h2><div class="form-row"><label>编码<input v-model.trim="hardwareForm.hardwareCode" /></label><label>名称<input v-model.trim="hardwareForm.hardwareName" /></label><label>单位<input v-model.trim="hardwareForm.unit" /></label><label>报价<input v-model.number="hardwareForm.salePrice" type="number" /></label><label>库存模式<select v-model="hardwareForm.stockMode"><option value="STOCK">常用库存</option><option value="PURCHASE">按单采购</option></select></label><button class="primary align-end" @click="addHardware">新增</button></div><div class="table-wrap"><table><thead><tr><th>编码</th><th>名称</th><th>模式</th><th>报价</th><th>现有库存</th><th>可用库存</th></tr></thead><tbody><tr v-for="h in hardware" :key="h.id"><td>{{ h.hardware_code }}</td><td>{{ h.hardware_name }}</td><td>{{ h.stock_mode }}</td><td>{{ money(h.sale_price) }}</td><td>{{ h.on_hand_quantity || 0 }}</td><td>{{ h.available_quantity || 0 }}</td></tr></tbody></table></div></div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import * as api from '../api/contract'

const props = defineProps({ view: { type: String, default: '客户订单' } })
const viewTabs = {
  客户订单: '客户订单', CAD评审池: 'CAD评审', 拆单: '拆单', 工厂订单: '工厂订单', 补单管理: '工厂订单',
  报价订单池: '工厂订单', 完整报价明细: '工厂订单', 报价版本: '工厂订单', 客户报价汇总: '商务财务', 报价PDF与客户确认: '商务财务',
  付款计划: '商务财务', 到账确认: '商务财务', 价格调整审批: '商务财务', 下料放行: '商务财务', 月结与逾期: '商务财务',
  生产线配置: '配置', 五金资料: '配置', 库存管理: '配置', 收款账户配置: '配置',
}
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
const products = ref([])
const materials = ref([])
const quoteRules = ref([])
const quotePdfs = ref([])
const companyProfile = ref({ companyName: '龙泽伟尼', logoPath: '', companyAddress: '', contactPhone: '' })
const selectedCustomerOrderId = ref('')
const quoteFactory = ref(null)
const externalQuoteFactory = ref(null)
const externalQuoteVersions = ref([])
const orderFiles = ref([])
const enabledLines = computed(() => lines.value.filter(l => Number(l.enabled) === 1))
const enabledAccounts = computed(() => accounts.value.filter(a => Number(a.enabled) === 1))

const orderForm = ref({ customerId: '', customerOrderNo: '', remark: '' })
const externalQuoteForm = ref({ externalQuoteNo: '', finalAmount: 0, quoteDate: new Date().toISOString().slice(0, 10), attachmentPath: '', customerConfirmed: false, remark: '' })
const draftForm = ref({ factoryOrderName: '', productionLineId: '', orderType: 'NORMAL', parentFactoryOrderId: '', remark: '' })
const adjustmentForm = ref({ finalAmount: 0, reason: '' })
const confirmationForm = ref({ pdfId: '', confirmationMethod: '微信', customerContact: '', confirmationRemark: '' })
const pdfForm = ref({ taxRate: 0, validDays: 15, quoteRemark: '' })
const paymentPlanForm = ref({ settlementType: 'FULL', amount: 0, dueDate: '' })
const receiptForm = ref({ actualAmount: 0, paymentMethod: '对公账户', receivingAccountId: '', payerName: '' })
const releaseForm = ref({ requestType: '正常下料', bossVerbalApproved: false, requestReason: '' })
const lineForm = ref({ lineCode: '', lineName: '', quoteMode: 'ERP' })
const accountForm = ref({ accountCode: '', accountName: '', paymentMethod: '', bankName: '' })
const hardwareForm = ref({ hardwareCode: '', hardwareName: '', unit: '个', salePrice: 0, stockMode: 'STOCK' })
const blankQuoteItem = () => ({ sourceType: 'PRODUCT', productCategory: '', productName: '', specification: '', materialStructure: '', handleColor: '', widthMm: 0, heightMm: 0, lengthMm: 0, thicknessMm: 0, quantity: 1, hingeHole: '', processDesc: '', attachmentName: '', attachmentPath: '', unit: '', pricingMode: 'AREA', minBillQuantity: 0, baseUnitPrice: 0, selectedRuleIds: [], customRules: [], discountEligible: true, nonDiscountRuleIds: [], nonDiscountCustomRuleNames: [], productionProcess: '', technician: '', productId: '', hardwareItemId: '', newRule: { ruleCategory: '', ruleName: '', chargeReason: '', adjustMode: 'FIXED_PER_M2', adjustValue: 0, unitDesc: '元/平方米', discountEligible: true } })
const quoteForm = ref({ discountRate: 1, discountReason: '', quoteDesc: '', items: [blankQuoteItem()] })

watch(() => props.view, async (view) => {
  const tab = viewTabs[view] || '客户订单'
  activeTab.value = tab
  await refreshCurrentView(tab)
}, { immediate: true })

async function refreshCurrentView(tab = activeTab.value) {
  if (tab === '客户订单') {
    await Promise.all([loadCustomers(), loadOrders()])
    return
  }
  if (tab === 'CAD评审') {
    await loadReviewPool()
    return
  }
  if (tab === '拆单') {
    await Promise.all([loadOrders(), loadLines(), loadFactoryOrders()])
    if (selectedCustomerOrderId.value) await loadDrafts()
    return
  }
  if (tab === '工厂订单') {
    await loadFactoryOrders()
    return
  }
  if (tab === '商务财务') {
    await Promise.all([loadCommercial(), loadAccounts()])
    return
  }
  if (tab === '配置') {
    await Promise.all([loadLines(), loadAccounts(), loadHardware(), loadCompanyProfile()])
  }
}
async function loadCustomers() { customers.value = (await api.listCustomers('')).data.data || [] }
async function loadOrders() { orderRows.value = (await api.listCustomerOrders(null, '')).data.data || [] }
async function loadReviewPool() { reviewPool.value = (await api.listReviewPoolV3()).data.data || [] }
async function loadLines() { lines.value = (await api.listProductionLines()).data.data || [] }
async function loadFactoryOrders() { factoryOrders.value = (await api.listFactoryOrdersV3()).data.data || [] }
async function loadCommercial() { commercialOrders.value = (await api.listCommercialOrdersV3()).data.data || []; queues.value = (await api.getOrderFlowWorkQueues()).data.data || {} }
async function loadAccounts() { accounts.value = (await api.listReceivingAccountsV3()).data.data || [] }
async function loadHardware() { hardware.value = (await api.listHardwareItemsV3('')).data.data || [] }
async function loadCatalogs() { const [p,m]=await Promise.all([api.listProducts(''),api.listMaterials('')]); products.value=p.data.data||[]; materials.value=m.data.data||[] }
async function loadQuoteRules() { quoteRules.value = (await api.listQuoteRules('', '')).data.data || [] }
async function loadCompanyProfile() { const row = (await api.getCompanyProfileV3()).data.data || {}; companyProfile.value = { companyName: row.company_name || '', logoPath: row.logo_path || '', companyAddress: row.company_address || '', contactPhone: row.contact_phone || '' } }
function customerName(id) { return customers.value.find(c => String(c.id) === String(id))?.customerName || '-' }
function money(value) { return Number(value || 0).toFixed(2) }
function pricingModeText(mode) { return ({ AREA: '按面积', LENGTH: '按长度', COUNT: '按数量' })[String(mode || 'AREA').toUpperCase()] || '按面积' }
function message(error) { window.alert(error?.response?.data?.message || error?.message || '操作失败') }

function selectOrderFiles(event) { orderFiles.value = Array.from(event.target.files || []) }
async function createOrder() {
  const duplicate = orderRows.value.some(o => String(o.customerOrderNo || '').trim().toLowerCase() === orderForm.value.customerOrderNo.trim().toLowerCase())
  if (duplicate) return window.alert('客户订单号已存在，不允许重复创建')
  if (!orderForm.value.customerId || !orderForm.value.customerOrderNo || !orderFiles.value.length) return window.alert('客户、客户订单号和CAD文件必须全部填写')
  try { await api.createCustomerOrderWithFiles({ ...orderForm.value, customerId: Number(orderForm.value.customerId) }, orderFiles.value); orderForm.value = { customerId: '', customerOrderNo: '', remark: '' }; orderFiles.value = []; await Promise.all([loadOrders(), loadReviewPool()]) } catch (e) { message(e) }
}
async function uploadCad(id, event) { const file = event.target.files?.[0]; if (!file) return; try { await api.uploadCustomerOrderCad(id, file); window.alert('CAD已上传') } catch (e) { message(e) } finally { event.target.value = '' } }
async function claimReview(id) { try { await api.claimCustomerOrderReview(id); await loadReviewPool(); await loadOrders() } catch (e) { message(e) } }
async function selectForSplit(order) { selectedCustomerOrderId.value = String(order.id); activeTab.value = '拆单'; await Promise.all([loadOrders(), loadLines(), loadFactoryOrders()]); await loadDrafts() }
async function loadDrafts() { if (!selectedCustomerOrderId.value) { drafts.value = []; return }; drafts.value = (await api.listSplitDrafts(Number(selectedCustomerOrderId.value))).data.data || [] }
async function saveDraft() { try { await api.addSplitDraft(Number(selectedCustomerOrderId.value), { ...draftForm.value, productionLineId: Number(draftForm.value.productionLineId), parentFactoryOrderId: draftForm.value.parentFactoryOrderId || null }); draftForm.value = { factoryOrderName: '', productionLineId: '', orderType: 'NORMAL', parentFactoryOrderId: '', remark: '' }; await loadDrafts() } catch (e) { message(e) } }
async function submitSplit() { try { await api.submitCustomerOrderSplit(Number(selectedCustomerOrderId.value)); await Promise.all([loadDrafts(), loadFactoryOrders(), loadOrders()]) } catch (e) { message(e) } }
async function claimFactory(id) { try { const row=(await api.claimFactoryOrderV3(id)).data.data; await loadFactoryOrders(); await openQuote(row) } catch (e) { message(e) } }
async function assignFactory(order) { const userId = window.prompt('请输入新的报价负责人用户ID'); if (!userId) return; const reason = window.prompt('填写指定或转交原因') || ''; try { await api.assignFactoryOrderV3(order.factory_order_id, { userId: Number(userId), reason }); await loadFactoryOrders() } catch (e) { message(e) } }
async function openQuote(order) { await Promise.all([loadCatalogs(), loadHardware(), loadQuoteRules()]); quoteFactory.value = order; quoteForm.value = { discountRate: Number(order.current_quote_version ? order.discount_rate : (order.customer_discount_rate || 1)), discountReason: '', quoteDesc: '', items: [blankQuoteItem()] }; const versions = (await api.listFactoryOrderQuotesV3(order.factory_order_id)).data.data || []; if (versions.length) { const full = (await api.getFactoryOrderQuoteV3(versions[0].id)).data.data; quoteForm.value = { discountRate: Number(full.discount_rate || 1), discountReason: '', quoteDesc: full.quote_desc || '', items: (full.items || []).map(savedQuoteItem) } } }
async function requestDiscount() { try { await api.requestFactoryOrderDiscountV3(quoteFactory.value.factory_order_id, { requestedRate: Number(quoteForm.value.discountRate), reason: quoteForm.value.discountReason }); window.alert('临时折扣已提交审批') } catch(e){ message(e) } }
async function approveDiscount(id, approved) { try { await api.approveFactoryOrderDiscountV3(id, { approved, remark: '' }); await loadCommercial() } catch(e){ message(e) } }
async function openExternalQuote(order) { externalQuoteFactory.value = order; externalQuoteForm.value = { externalQuoteNo: '', finalAmount: Number(order.discounted_quote_amount || 0), quoteDate: new Date().toISOString().slice(0, 10), attachmentPath: '', customerConfirmed: false, remark: '' }; externalQuoteVersions.value = (await api.listFactoryOrderExternalQuotesV3(order.factory_order_id)).data.data || [] }
async function uploadExternalQuoteFile(event) { const file=event.target.files?.[0]; if(!file)return; try { const row=(await api.uploadQuoteAttachmentV3(file)).data.data; externalQuoteForm.value.attachmentPath=row.filePath } catch(e){ message(e) } }
async function saveExternalQuote() { try { await api.saveFactoryOrderExternalQuoteV3(externalQuoteFactory.value.factory_order_id, { ...externalQuoteForm.value, confirmedAt: externalQuoteForm.value.customerConfirmed ? new Date().toISOString() : null }); await Promise.all([loadFactoryOrders(), openExternalQuote(externalQuoteFactory.value), loadCommercial()]); window.alert('外部报价版本已保存') } catch(e){ message(e) } }
function addQuoteItem() { quoteForm.value.items.push(blankQuoteItem()) }
function onQuoteSource(item) { const fresh=blankQuoteItem(); Object.assign(item, fresh, { sourceType: item.sourceType }) }
function selectCatalogItem(item) { if (item.sourceType === 'HARDWARE') { const h = hardware.value.find(x => String(x.id) === String(item.hardwareItemId)); if (!h) return; item.productId=''; item.productCategory='HARDWARE'; item.productName=h.hardware_name; item.baseUnitPrice=Number(h.sale_price||0); item.unit=h.unit||'个'; item.pricingMode='COUNT'; item.discountEligible=false; return } const p=products.value.find(x=>String(x.id)===String(item.productId)); if(!p)return; item.hardwareItemId=''; item.productCategory=p.type||''; item.productName=p.product_name||p.model||''; item.baseUnitPrice=Number(p.unit_price||0); item.unit=p.unit_price_unit||p.unit||''; item.pricingMode=String(p.pricing_mode||'AREA').toUpperCase(); item.minBillQuantity=Number(p.min_bill_quantity||0); item.discountEligible=Number(p.discount_eligible ?? 1)===1; item.materialStructure=p.material_name||item.materialStructure; item.handleColor=p.handle_color||item.handleColor; item.thicknessMm=Number(p.thickness||item.thicknessMm||0) }
function toggleRule(item, rule, event) { if (event.target.checked) item.selectedRuleIds.push(rule.id); else { item.selectedRuleIds = item.selectedRuleIds.filter(id => id !== rule.id); item.nonDiscountRuleIds = item.nonDiscountRuleIds.filter(id => id !== rule.id) } }
function toggleRuleDiscount(item, rule, event) { if (event.target.checked) item.nonDiscountRuleIds = item.nonDiscountRuleIds.filter(id => id !== rule.id); else if (!item.nonDiscountRuleIds.includes(rule.id)) item.nonDiscountRuleIds.push(rule.id) }
function addCustomRule(item) { const units={PERCENT:'%',FIXED_PER_ITEM:'元/件',FIXED_PER_METER:'元/米',FIXED_ONCE:'元/次',FIXED_PER_M2:'元/平方米'}; const r = { ...item.newRule, sourceRuleId: null, unitDesc: units[item.newRule.adjustMode], ruleQuantity: item.quantity }; if (!r.ruleName) return; item.customRules.push(r); if (!r.discountEligible) item.nonDiscountCustomRuleNames.push(r.ruleName); item.newRule = { ruleCategory: '', ruleName: '', chargeReason: '', adjustMode: 'FIXED_PER_M2', adjustValue: 0, unitDesc: '元/平方米', discountEligible: true } }
function removeCustomRule(item, index) { const [r] = item.customRules.splice(index,1); item.nonDiscountCustomRuleNames = item.nonDiscountCustomRuleNames.filter(name => name !== r.ruleName) }
async function uploadQuoteImage(item,event) { const file=event.target.files?.[0]; if(!file) return; try { const row=(await api.uploadQuoteAttachmentV3(file)).data.data; item.attachmentName=row.fileName; item.attachmentPath=row.filePath } catch(e){ message(e) } }
function savedQuoteItem(row) { const item=blankQuoteItem(); Object.assign(item,{ sourceType:row.hardware_item_id?'HARDWARE':'PRODUCT',productCategory:row.product_category,productName:row.product_name,specification:row.specification||'',materialStructure:row.material_structure||'',handleColor:row.handle_color||'',widthMm:Number(row.width_mm||0),heightMm:Number(row.height_mm||0),lengthMm:Number(row.length_mm||0),thicknessMm:Number(row.thickness_mm||0),quantity:Number(row.quantity||1),hingeHole:row.hinge_hole||'',processDesc:row.process_desc||'',attachmentName:row.attachment_name||'',attachmentPath:row.attachment_path||'',unit:row.unit||'',pricingMode:row.pricing_mode||'AREA',baseUnitPrice:Number(row.base_unit_price||0),selectedRuleIds:String(row.selected_rule_ids||'').split(',').filter(Boolean).map(Number),customRules:row.custom_rule_json ? JSON.parse(row.custom_rule_json) : [],discountEligible:Number(row.discount_eligible)===1,nonDiscountRuleIds:(row.extra_prices||[]).filter(x=>Number(x.discount_eligible)===0&&x.source_rule_id).map(x=>Number(x.source_rule_id)),nonDiscountCustomRuleNames:(row.extra_prices||[]).filter(x=>Number(x.discount_eligible)===0&&!x.source_rule_id).map(x=>x.rule_name),productionProcess:row.production_process||'',technician:row.technician||'',productId:row.product_id||'',hardwareItemId:row.hardware_item_id||'' }); return item }
async function submitQuote(submit) { try { await api.saveFactoryOrderQuoteV3(quoteFactory.value.factory_order_id, { ...quoteForm.value, submit }); if(submit) quoteFactory.value = null; await Promise.all([loadFactoryOrders(), loadCommercial()]); window.alert(submit?'报价版本已提交':'报价草稿已保存') } catch (e) { message(e) } }
async function openCommercial(order) { commercialDetail.value = (await api.getCommercialDetailV3(order.id)).data.data || {}; adjustmentForm.value.finalAmount = Number(order.final_receivable_amount || order.quote_total_amount || 0); pdfForm.value.taxRate=Number(commercialDetail.value.order?.tax_rate||0); pdfForm.value.validDays=Number(commercialDetail.value.order?.quote_valid_days||15); quotePdfs.value=(await api.listCustomerQuotePdfsV3(order.id)).data.data||[] }
async function submitAdjustment() { try { await api.requestPriceAdjustmentV3(commercialDetail.value.order.id, adjustmentForm.value); await loadCommercial() } catch (e) { message(e) } }
async function approveAdjustment(id, approved) { try { await api.approvePriceAdjustmentV3(id, { approved, remark: '' }); await loadCommercial() } catch (e) { message(e) } }
async function confirmQuote() { try { await api.confirmCustomerQuoteV3(commercialDetail.value.order.id, { ...confirmationForm.value, pdfId:Number(confirmationForm.value.pdfId), confirmedAt: new Date().toISOString(), attachmentPath: null }); await loadCommercial(); await openCommercial({id:commercialDetail.value.order.id,final_receivable_amount:commercialDetail.value.order.final_receivable_amount}) } catch (e) { message(e) } }
async function generatePdf() { try { await api.generateCustomerQuotePdfV3(commercialDetail.value.order.id,pdfForm.value); quotePdfs.value=(await api.listCustomerQuotePdfsV3(commercialDetail.value.order.id)).data.data||[] } catch(e){ message(e) } }
async function downloadPdf(row) { try { const response=await api.downloadCustomerQuotePdfV3(row.id); const url=URL.createObjectURL(response.data); const a=document.createElement('a'); a.href=url; a.download=`${commercialDetail.value.order.customer_order_no}_报价单_V${row.pdf_version}.pdf`; a.click(); URL.revokeObjectURL(url) } catch(e){ message(e) } }
async function saveCompanyProfile() { try { await api.updateCompanyProfileV3(companyProfile.value); window.alert('公司资料已保存') } catch(e){ message(e) } }
async function uploadCompanyLogo(event) { const file=event.target.files?.[0]; if(!file)return; try { const row=(await api.uploadQuoteAttachmentV3(file)).data.data; companyProfile.value.logoPath=row.filePath } catch(e){ message(e) } }
async function submitPaymentPlan() { try { await api.createPaymentPlanV3(commercialDetail.value.order.id, { settlementType: paymentPlanForm.value.settlementType, customerConfirmed: true, installments: [{ installmentNo: 1, plannedAmount: Number(paymentPlanForm.value.amount), dueDate: paymentPlanForm.value.dueDate }] }); await loadCommercial() } catch (e) { message(e) } }
async function submitReceipt() { try { await api.submitPaymentReceiptV3(commercialDetail.value.order.id, { ...receiptForm.value, receivingAccountId: receiptForm.value.receivingAccountId ? Number(receiptForm.value.receivingAccountId) : null, receivedAt: new Date().toISOString(), paymentPlanId: null, bankReference: '', voucherPath: null }); await loadCommercial() } catch (e) { message(e) } }
async function confirmReceipt(id, approved) { try { await api.confirmPaymentReceiptV3(id, { approved, remark: '' }); await loadCommercial() } catch (e) { message(e) } }
async function requestRelease() { try { await api.requestCuttingReleaseV3(commercialDetail.value.order.id, { ...releaseForm.value, bossConfirmedAt: releaseForm.value.bossVerbalApproved ? new Date().toISOString() : null }); await loadCommercial() } catch (e) { message(e) } }
async function financeRelease(id, approved) { try { await api.financeConfirmCuttingReleaseV3(id, { approved, remark: '' }); await loadCommercial() } catch (e) { message(e) } }
async function approveRelease(id, approved) { try { await api.directorApproveCuttingReleaseV3(id, { approved, remark: '' }); await loadCommercial() } catch (e) { message(e) } }
async function releaseCutting(id) { try { await api.releaseToCuttingV3(id); await Promise.all([loadCommercial(), loadFactoryOrders()]) } catch (e) { message(e) } }
async function addLine() { try { await api.createProductionLineV3({ ...lineForm.value, enabled: true, sortOrder: lines.value.length * 10 + 10 }); lineForm.value = { lineCode: '', lineName: '', quoteMode: 'ERP' }; await loadLines() } catch (e) { message(e) } }
async function addAccount() { try { await api.createReceivingAccountV3({ ...accountForm.value, enabled: true, sortOrder: accounts.value.length * 10 + 10, accountNoMasked: '' }); accountForm.value = { accountCode: '', accountName: '', paymentMethod: '', bankName: '' }; await loadAccounts() } catch (e) { message(e) } }
async function addHardware() { try { await api.createHardwareItemV3({ ...hardwareForm.value, enabled: true, specification: '' }); hardwareForm.value = { hardwareCode: '', hardwareName: '', unit: '个', salePrice: 0, stockMode: 'STOCK' }; await loadHardware() } catch (e) { message(e) } }
</script>

<style scoped>
.flow-page{display:grid;gap:14px}.flow-tabs{display:flex;gap:4px;border-bottom:1px solid #d7dce2}.flow-tabs button{border:0;background:transparent;padding:10px 14px}.flow-tabs button.active{color:#116149;border-bottom:2px solid #116149;font-weight:700}.flow-section{display:grid;gap:14px}.section-head{display:flex;align-items:center;justify-content:space-between;gap:10px}.section-head h2,.config-band h2{font-size:17px;margin:0}.section-head>div{display:flex;gap:6px}.form-row{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:10px;align-items:end}.form-row label{display:grid;gap:5px;font-size:12px;color:#4d5965}.form-row input,.form-row select,.section-head select{width:100%;height:36px;border:1px solid #cfd6dd;padding:0 9px;background:#fff}.form-row button,.section-head button,.table-wrap button,.queue-row button,.action-bar button,.quote-item-editor button{height:34px;border:1px solid #c8d0d8;background:#fff;padding:0 11px}.primary{background:#116149!important;border-color:#116149!important;color:#fff}.align-end{align-self:end}.table-wrap{overflow:auto;border-top:1px solid #d9dee3}.table-wrap table{width:100%;border-collapse:collapse;white-space:nowrap}.table-wrap th,.table-wrap td{padding:9px 10px;border-bottom:1px solid #e4e8ec;text-align:left;font-size:12px}.table-wrap th{background:#f5f7f8;color:#52606c}.file-action{cursor:pointer;color:#116149}.file-action input{display:none}.action-bar{display:flex;justify-content:flex-end}.commercial-panel{border-top:1px solid #d9dee3;border-bottom:1px solid #d9dee3;padding:14px 0;display:grid;gap:12px}.commercial-panel h3{margin:0;font-size:15px}.check{display:flex!important;grid-template-columns:18px 1fr!important;align-items:center;height:36px}.check input{width:16px;height:16px}.quote-item-editor{border:1px solid #d9dee3;padding:10px;display:grid;gap:10px}.quote-item-head{display:flex;justify-content:space-between;align-items:center}.quote-fields{display:grid;grid-template-columns:repeat(5,minmax(120px,1fr));gap:8px}.quote-fields label{display:grid;gap:4px;font-size:12px}.quote-fields input,.quote-fields select,.custom-rule-row input,.custom-rule-row select{height:32px;border:1px solid #cfd6dd;padding:0 7px;min-width:0}.rule-area{border-top:1px solid #e1e5e9;padding-top:9px;display:grid;gap:8px}.rule-list{display:grid;grid-template-columns:repeat(2,minmax(260px,1fr));gap:5px}.rule-list label{font-size:12px;display:flex;align-items:center;gap:5px}.custom-rule-row{display:grid;grid-template-columns:2fr 1fr 1fr 1fr auto;gap:7px;align-items:center}.queue-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}.queue-grid>div{border-top:2px solid #87939d;padding-top:8px}.queue-grid h3{font-size:14px;margin:0 0 8px}.queue-row{display:flex;align-items:center;gap:6px;padding:7px 0;border-bottom:1px solid #e6eaed;font-size:12px}.queue-row span{flex:1}.config-band{display:grid;gap:10px;padding-bottom:14px;border-bottom:1px solid #d9dee3}.tag-list{display:flex;flex-wrap:wrap;gap:6px}.tag-list span{border:1px solid #d5dbe0;padding:5px 8px;font-size:12px}@media(max-width:1000px){.quote-fields{grid-template-columns:repeat(3,1fr)}.rule-list{grid-template-columns:1fr}}@media(max-width:700px){.flow-tabs{overflow:auto}.form-row,.quote-fields,.custom-rule-row{grid-template-columns:1fr}.queue-grid{grid-template-columns:1fr}}
</style>
