<template>
  <div class="customer-center">
    <template v-if="mode === 'list'">
      <section class="toolbar-card">
        <input v-model.trim="filters.keyword" class="keyword-input" placeholder="&#x641C;&#x7D22;&#x5BA2;&#x6237;&#x540D;&#x79F0; / &#x7F16;&#x7801; / &#x8054;&#x7CFB;&#x4EBA; / &#x7535;&#x8BDD;" @keyup.enter="loadCustomers" />
        <select v-model="filters.customerType">
          <option value="">&#x5168;&#x90E8;&#x5BA2;&#x6237;&#x7C7B;&#x578B;</option>
          <option v-for="item in customerTypeOptions" :key="item" :value="item">{{ item }}</option>
        </select>
        <select v-model="filters.cooperationStatus">
          <option value="">&#x5168;&#x90E8;&#x5408;&#x4F5C;&#x72B6;&#x6001;</option>
          <option v-for="item in statusOptions" :key="item" :value="item">{{ item }}</option>
        </select>
        <select v-model="filters.salesManager">
          <option value="">&#x5168;&#x90E8;&#x9500;&#x552E;&#x7ECF;&#x7406;</option>
          <option v-for="item in managerFilterOptions" :key="item" :value="item">{{ item }}</option>
        </select>
        <div class="toolbar-actions">
          <button class="ghost-btn" @click="resetFilters">&#x91CD;&#x7F6E;</button>
          <button class="ghost-btn" @click="exportCustomers">&#x5BFC;&#x51FA;</button>
          <button class="primary-btn" @click="startCreate">&#x65B0;&#x589E;&#x5BA2;&#x6237;</button>
        </div>
      </section>

      <section class="stats-grid">
        <article v-for="card in statsCards" :key="card.title" class="stat-card">
          <span class="stat-mark" :class="card.tone"></span>
          <div>
            <p>{{ card.title }}</p>
            <strong>{{ card.value }}</strong>
            <small>{{ card.desc }}</small>
          </div>
        </article>
      </section>

      <section class="list-card">
        <div class="list-summary">
          <div><strong>{{ filteredCustomers.length }}</strong> &#x4E2A;&#x5BA2;&#x6237;</div>
          <label class="sort-control">
            <span>&#x6392;&#x5E8F;</span>
            <select v-model="sortMode">
              <option value="lastOrderDesc">&#x6700;&#x8FD1;&#x4E0B;&#x5355;&#x4F18;&#x5148;</option>
              <option value="annualDesc">&#x5E74;&#x5EA6;&#x91D1;&#x989D;&#x4ECE;&#x591A;&#x5230;&#x5C11;</option>
              <option value="debtDesc">&#x6B20;&#x6B3E;&#x98CE;&#x9669;&#x4ECE;&#x9AD8;&#x5230;&#x4F4E;</option>
              <option value="statusRisk">&#x98CE;&#x9669;&#x5BA2;&#x6237;&#x4F18;&#x5148;</option>
              <option value="levelDesc">&#x5BA2;&#x6237;&#x7B49;&#x7EA7;&#x4ECE;&#x9AD8;&#x5230;&#x4F4E;</option>
              <option value="nameAsc">&#x5BA2;&#x6237;&#x540D;&#x79F0; A-Z</option>
              <option value="createdDesc">&#x6700;&#x8FD1;&#x5EFA;&#x6863;&#x4F18;&#x5148;</option>
            </select>
          </label>
        </div>
        <div v-if="filteredCustomers.length" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>&#x5BA2;&#x6237;&#x540D;&#x79F0;</th>
                <th>&#x5BA2;&#x6237;&#x7C7B;&#x578B;</th>
                <th>&#x6240;&#x5728;&#x5730;&#x533A;</th>
                <th>&#x8054;&#x7CFB;&#x4EBA;</th>
                <th>&#x7535;&#x8BDD;</th>
                <th>&#x9500;&#x552E;&#x7ECF;&#x7406;</th>
                <th>&#x5408;&#x4F5C;&#x72B6;&#x6001;</th>
                <th>&#x5BA2;&#x6237;&#x7B49;&#x7EA7;</th>
                <th>&#x7ED3;&#x7B97;&#x65B9;&#x5F0F;</th>
                <th>&#x5185;&#x90E8;&#x6298;&#x6263;</th>
                <th class="money-head">&#x5E74;&#x5EA6;&#x8BA2;&#x5355;&#x91D1;&#x989D;</th>
                <th class="money-head">&#x6B20;&#x6B3E;&#x91D1;&#x989D;</th>
                <th>&#x6700;&#x8FD1;&#x4E0B;&#x5355;</th>
                <th>&#x64CD;&#x4F5C;</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pagedCustomers" :key="row.id" @click="viewCustomer(row)">
                <td><strong>{{ row.regionName || '-' }}</strong><small>{{ row.customerCode || '-' }}</small></td>
                <td>{{ row.customerType || '-' }}</td>
                <td>{{ row.provinceCity || '-' }}</td>
                <td>{{ row.contact || '-' }}</td>
                <td>{{ maskPhone(row.phone) || '-' }}</td>
                <td>{{ row.salesManager || '-' }}</td>
                <td><span class="status-pill" :class="statusClass(row.cooperationStatus)">{{ row.cooperationStatus || '-' }}</span></td>
                <td><span class="level-pill" :class="levelClass(row.customerLevel)">{{ row.customerLevel || '-' }}</span></td>
                <td><span class="settle-pill">{{ row.settlementMethod || row.settleType || '-' }}</span></td>
                <td>{{ row.internalDiscount ? `${row.internalDiscount}%` : '-' }}</td>
                <td class="money-cell">{{ moneyText(row.annualOrderAmount) }}</td>
                <td class="money-cell" :class="{ overdue: Number(row.debtAmount || 0) > 0 }">{{ moneyText(row.debtAmount) }}</td>
                <td>{{ row.lastOrderDate || '-' }}</td>
                <td class="row-actions" @click.stop>
                  <button class="mini-btn" @click="viewCustomer(row)">&#x67E5;&#x770B;</button>
                  <button class="mini-btn" @click="startEdit(row)">&#x7F16;&#x8F91;</button>
                  <button class="mini-btn" @click="openReconcile(row)">&#x5BF9;&#x8D26;</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="filteredCustomers.length" class="pagination">
          <span>{{ pageStart }}-{{ pageEnd }} / {{ filteredCustomers.length }}</span>
          <select v-model.number="pageSize">
            <option :value="10">10 &#x6761;/&#x9875;</option>
            <option :value="20">20 &#x6761;/&#x9875;</option>
            <option :value="50">50 &#x6761;/&#x9875;</option>
          </select>
          <button class="ghost-btn page-btn" :disabled="currentPage <= 1" @click="currentPage -= 1">&#x4E0A;&#x4E00;&#x9875;</button>
          <span class="page-index">{{ currentPage }} / {{ totalPages }}</span>
          <button class="ghost-btn page-btn" :disabled="currentPage >= totalPages" @click="currentPage += 1">&#x4E0B;&#x4E00;&#x9875;</button>
          <input v-model.number="jumpPage" class="jump-input" type="number" min="1" :max="totalPages" />
          <button class="ghost-btn page-btn" @click="goJumpPage">&#x8DF3;&#x8F6C;</button>
        </div>
        <div v-else class="empty-state">
          <div class="empty-illu">&#x5BA2;</div>
          <h3>&#x6682;&#x65E0;&#x5BA2;&#x6237;&#x6863;&#x6848;</h3>
          <p>&#x65B0;&#x589E;&#x7B2C;&#x4E00;&#x4E2A;&#x5BA2;&#x6237;&#x540E;&#xFF0C;&#x8FD9;&#x91CC;&#x4F1A;&#x5C55;&#x793A;&#x5BA2;&#x6237;&#x57FA;&#x7840;&#x8D44;&#x6599;&#x548C;&#x4E1A;&#x52A1;&#x4FE1;&#x606F;&#x3002;</p>
          <button class="primary-btn" @click="startCreate">&#x65B0;&#x589E;&#x5BA2;&#x6237;</button>
        </div>
      </section>
    </template>

    <template v-else-if="mode === 'detail'">
      <section class="detail-action-bar">
        <button class="ghost-btn" @click="backToList">&#8592; &#x8FD4;&#x56DE;&#x5217;&#x8868;</button>
        <button class="primary-btn" @click="startEdit(detailRow)">&#9998; &#x7F16;&#x8F91;&#x5BA2;&#x6237;</button>
      </section>

      <section class="profile-hero-card">
        <div class="profile-hero-top">
          <div class="profile-company">
            <div class="profile-avatar">&#x4F01;</div>
            <div class="profile-title">
              <h3>{{ detailRow.regionName || '-' }}</h3>
              <p class="profile-address">&#x1F4CD; {{ detailRow.address || '-' }}</p>
              <div class="code-badge-row">
                <button class="code-chip" @click="copyCustomerCode(detailRow.customerCode)">NO. {{ detailRow.customerCode || '-' }} <span>&#x2398;</span></button>
                <div class="profile-badges">
                  <span class="type-badge type-blue">{{ detailRow.customerType || '-' }}</span>
                  <span class="level-pill" :class="levelClass(detailRow.customerLevel)">{{ detailRow.customerLevel || '-' }}</span>
                  <span class="settle-pill">{{ detailRow.settlementMethod || '-' }}</span>
                  <span class="status-pill" :class="statusClass(detailRow.cooperationStatus)">{{ detailRow.cooperationStatus || '&#x672A;&#x8BBE;&#x7F6E;' }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="hero-remark">
            <div class="detail-card-head"><h3>&#x5907;&#x6CE8;</h3><span>&#x5185;&#x90E8;&#x5907;&#x6CE8;</span></div>
            <p>{{ detailRow.remark || '-' }}</p>
          </div>
        </div>

        <div class="contact-summary">
          <article v-for="item in contactSummary" :key="item.label">
            <span class="summary-icon">{{ item.icon }}</span>
            <div><p>{{ item.label }}</p><strong>{{ item.value || '-' }}</strong></div>
          </article>
        </div>
      </section>

      <section class="detail-columns">
        <article class="detail-info-card">
          <h3><span class="card-title-icon">&#x25A3;</span>&#x57FA;&#x7840;&#x8D44;&#x6599;</h3>
          <div class="info-list">
            <p v-for="item in baseInfoItems" :key="item.label"><span>{{ item.label }}</span><strong>{{ item.value || '-' }}</strong></p>
          </div>
        </article>

        <article class="detail-info-card">
          <h3><span class="card-title-icon">&#x25C7;</span>&#x5408;&#x4F5C;&#x4E0E;&#x7ED3;&#x7B97;</h3>
          <div class="info-list">
            <p v-for="item in cooperationInfoItems" :key="item.label">
              <span>{{ item.label }}</span>
              <strong v-if="item.kind === 'status'"><i class="state-dot" :class="statusClass(detailRow.cooperationStatus)"></i>{{ item.value || '-' }}</strong>
              <strong v-else-if="item.kind === 'level'"><em class="inline-badge">{{ item.value || '-' }}</em></strong>
              <strong v-else>{{ item.value || '-' }}</strong>
            </p>
          </div>
        </article>

        <article class="detail-info-card stats-info-card">
          <div class="detail-card-head">
            <h3><span class="card-title-icon">&#xFFE5;</span>&#x4EA4;&#x6613;&#x7EDF;&#x8BA1;&#xFF08;&#x53EA;&#x8BFB;&#xFF09;</h3>
            <span>&#x7531;&#x8BA2;&#x5355; / &#x6536;&#x6B3E; / &#x5BF9;&#x8D26;&#x6A21;&#x5757;&#x81EA;&#x52A8;&#x6C47;&#x603B;</span>
          </div>
          <div class="mini-stat-grid">
            <article v-for="item in detailStats" :key="item.label" class="mini-stat" :class="{ danger: item.danger }">
              <span>{{ item.icon }}</span>
              <p>{{ item.label }}</p>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </article>
      </section>
    </template>

    <template v-else>
      <section class="form-page">
        <section class="subpage-actions">
          <div class="subpage-title">
            <h3 v-if="formMode === 'create'">&#x65B0;&#x589E;&#x5BA2;&#x6237;&#x6863;&#x6848;</h3>
            <h3 v-else>&#x7F16;&#x8F91;&#x5BA2;&#x6237;&#x6863;&#x6848;</h3>
            <p>OEM&#x5DE5;&#x5382; To B &#x5BA2;&#x6237;&#x8D44;&#x6599;&#x4E0E;&#x5408;&#x4F5C;&#x653F;&#x7B56;&#x7EF4;&#x62A4;</p>
          </div>
          <button class="ghost-btn" @click="backToList">&#x8FD4;&#x56DE;&#x5217;&#x8868;</button>
        </section>

        <section class="customer-overview-card">
          <div class="company-avatar">&#x4F01;</div>
          <div class="overview-main">
            <strong>{{ form.regionName || '&#x672A;&#x586B;&#x5199;&#x5BA2;&#x6237;&#x540D;&#x79F0;' }}</strong>
            <span>{{ form.customerCode || '&#x7CFB;&#x7EDF;&#x4FDD;&#x5B58;&#x540E;&#x751F;&#x6210;' }}</span>
          </div>
          <span class="status-pill" :class="statusClass(form.cooperationStatus)">{{ form.cooperationStatus || '&#x5F85;&#x5B8C;&#x5584;' }}</span>
        </section>

        <section class="form-card">
          <h3>A. &#x57FA;&#x7840;&#x8D44;&#x6599;</h3>
          <div class="form-grid">
            <label class="field-cell required">
              <span>&#x5BA2;&#x6237;&#x540D;&#x79F0;</span>
              <input v-model.trim="form.regionName" placeholder="&#x8BF7;&#x8F93;&#x5165;&#x5BA2;&#x6237;&#x516C;&#x53F8;&#x6216;&#x95E8;&#x5E97;&#x540D;&#x79F0;" />
            </label>
            <label class="field-cell">
              <span>&#x5BA2;&#x6237;&#x7F16;&#x7801;</span>
              <input v-model.trim="form.customerCode" class="readonly-input" placeholder="&#x7CFB;&#x7EDF;&#x81EA;&#x52A8;&#x751F;&#x6210;" readonly />
            </label>
            <label class="field-cell required">
              <span>&#x7701;&#x4EFD;</span>
              <select v-model="form.province"><option value="">&#x8BF7;&#x9009;&#x62E9;&#x7701;&#x4EFD;</option><option v-for="item in provinceOptions" :key="item" :value="item">{{ item }}</option></select>
            </label>
            <label class="field-cell required">
              <span>&#x5E02;&#x533A;</span>
              <select v-model="form.city"><option value="">&#x8BF7;&#x9009;&#x62E9;&#x5E02;&#x533A;</option><option v-for="item in cityOptions" :key="item" :value="item">{{ item }}</option></select>
            </label>
            <label class="field-cell">
              <span>&#x5BA2;&#x6237;&#x7C7B;&#x578B;</span>
              <select v-model="form.customerType"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in customerTypeOptions" :key="item" :value="item">{{ item }}</option></select>
            </label>
            <label class="field-cell required">
              <span>&#x5408;&#x4F5C;&#x72B6;&#x6001;</span>
              <select v-model="form.cooperationStatus"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in statusOptions" :key="item" :value="item">{{ item }}</option></select>
            </label>
            <label class="field-cell required">
              <span>&#x8054;&#x7CFB;&#x4EBA;</span>
              <input v-model.trim="form.contact" placeholder="&#x8BF7;&#x8F93;&#x5165;&#x8054;&#x7CFB;&#x4EBA;" />
            </label>
            <label class="field-cell required">
              <span>&#x7535;&#x8BDD;</span>
              <input v-model.trim="form.phone" placeholder="&#x8BF7;&#x8F93;&#x5165;&#x7535;&#x8BDD;" />
            </label>
            <label class="field-cell required">
              <span>&#x9500;&#x552E;&#x7ECF;&#x7406;</span>
              <select v-model="form.salesManager"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in staffOptions" :key="item" :value="item">{{ item }}</option></select>
            </label>
            <label class="field-cell required">
              <span>&#x5BA2;&#x670D;&#x5458;</span>
              <select v-model="form.serviceStaff"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in serviceOptions" :key="item" :value="item">{{ item }}</option></select>
            </label>
            <label class="field-cell span-4">
              <span>&#x8BE6;&#x7EC6;&#x5730;&#x5740;</span>
              <small class="field-hint">填写格式：省 + 市 + 区 + 详细地址</small>
              <textarea v-model.trim="form.address" class="address-textarea" placeholder="请填写详细地址，例如：辽宁省辽阳市辽阳县首山镇人民街xx号"></textarea>
            </label>
          </div>
        </section>

        <section class="form-card">
          <h3>B. &#x5408;&#x4F5C;&#x653F;&#x7B56;</h3>
          <div class="form-grid">
            <label class="field-cell"><span>&#x5BA2;&#x6237;&#x7B49;&#x7EA7;</span><select v-model="form.customerLevel"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in policyLevelOptions" :key="item" :value="item">{{ item }}</option></select></label>
            <label class="field-cell"><span>&#x5408;&#x4F5C;&#x6A21;&#x5F0F;</span><select v-model="form.cooperationMode"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in cooperationModeOptions" :key="item" :value="item">{{ item }}</option></select></label>
            <label class="field-cell"><span>&#x5185;&#x90E8;&#x6298;&#x6263;&#xFF08;%&#xFF09;</span><input v-model.trim="form.internalDiscount" type="number" min="0" max="100" placeholder="90 / 95 / 100" /></label>
            <label class="field-cell"><span>&#x9002;&#x7528;&#x62A5;&#x4EF7;&#x4F53;&#x7CFB;</span><select v-model="form.quoteSystem"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in quoteSystemOptions" :key="item" :value="item">{{ item }}</option></select></label>
            <label class="field-cell"><span>&#x5408;&#x4F5C;&#x5F00;&#x59CB;&#x65F6;&#x95F4;</span><input v-model="form.cooperationStartDate" type="date" /></label>
            <div class="field-cell">
              <span>&#x662F;&#x5426;&#x542F;&#x7528;</span>
              <div class="radio-row"><label><input v-model="form.enabled" type="radio" value="&#x542F;&#x7528;" /> &#x542F;&#x7528;</label><label><input v-model="form.enabled" type="radio" value="&#x505C;&#x7528;" /> &#x505C;&#x7528;</label></div>
            </div>
          </div>
        </section>

        <section class="form-card">
          <h3>C. &#x7ED3;&#x7B97;&#x4E0E;&#x7269;&#x6D41;</h3>
          <div class="form-grid">
            <label class="field-cell"><span>&#x7ED3;&#x7B97;&#x65B9;&#x5F0F;</span><select v-model="form.settlementMethod"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in settlementOptions" :key="item" :value="item">{{ item }}</option></select></label>
            <label class="field-cell"><span>&#x8D26;&#x671F;&#x5929;&#x6570;</span><select v-model="form.creditDays"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in creditDayOptions" :key="item" :value="item">{{ item }}</option></select></label>
            <label class="field-cell"><span>&#x4ED8;&#x6B3E;&#x65B9;&#x5F0F;</span><select v-model="form.paymentMethod"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in paymentOptions" :key="item" :value="item">{{ item }}</option></select></label>
            <label class="field-cell"><span>&#x9ED8;&#x8BA4;&#x7269;&#x6D41;</span><select v-model="form.logistics"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in logisticsOptions" :key="item" :value="item">{{ item }}</option></select></label>
            <label class="field-cell"><span>&#x7269;&#x6D41;&#x8D39;&#x7528;&#x627F;&#x62C5;&#x65B9;</span><select v-model="form.freightPayer"><option value="">&#x8BF7;&#x9009;&#x62E9;</option><option v-for="item in freightPayerOptions" :key="item" :value="item">{{ item }}</option></select></label>
            <div class="field-cell">
              <span>&#x662F;&#x5426;&#x5F00;&#x7968;</span>
              <div class="radio-row"><label><input v-model="form.invoiceEnabled" type="radio" value="&#x662F;" /> &#x662F;</label><label><input v-model="form.invoiceEnabled" type="radio" value="&#x5426;" /> &#x5426;</label></div>
            </div>
          </div>
        </section>

        <section class="form-card readonly-card">
          <div class="card-title-row">
            <h3>D. &#x4EA4;&#x6613;&#x7EDF;&#x8BA1;</h3>
            <span>&#x4EE5;&#x4E0B;&#x6570;&#x636E;&#x7531;&#x8BA2;&#x5355;&#x3001;&#x6536;&#x6B3E;&#x3001;&#x5BF9;&#x8D26;&#x6A21;&#x5757;&#x81EA;&#x52A8;&#x6C47;&#x603B;&#xFF0C;&#x4E0D;&#x53EF;&#x624B;&#x52A8;&#x7F16;&#x8F91;&#x3002;</span>
          </div>
          <div class="readonly-stats">
            <article v-for="item in readonlyStats" :key="item.label" class="readonly-stat" :class="{ danger: item.danger }">
              <span class="readonly-icon">{{ item.icon }}</span>
              <div><p>{{ item.label }}</p><strong>{{ item.value }}</strong></div>
            </article>
          </div>
        </section>

        <section class="form-card">
          <div class="card-title-row">
            <h3>E. &#x5907;&#x6CE8;</h3>
            <span>&#x4EC5;&#x5185;&#x90E8;&#x53EF;&#x89C1; {{ remarkCount }}/500</span>
          </div>
          <label class="field-cell">
            <span>&#x5907;&#x6CE8;&#x4FE1;&#x606F;</span>
            <textarea v-model.trim="form.remark" maxlength="500" placeholder="&#x8BF7;&#x8F93;&#x5165;&#x5907;&#x6CE8;&#x4FE1;&#x606F;&#xFF08;&#x5982;&#x5BA2;&#x6237;&#x7279;&#x6B8A;&#x8981;&#x6C42;&#x3001;&#x5386;&#x53F2;&#x6C9F;&#x901A;&#x8BB0;&#x5F55;&#x7B49;&#xFF09;..."></textarea>
          </label>
        </section>

        <div class="form-actions">
          <button class="ghost-btn" @click="backToList">&#x53D6;&#x6D88;</button>
          <button class="primary-btn" :disabled="saving" @click="saveCustomer"><span v-if="saving">&#x4FDD;&#x5B58;&#x4E2D;...</span><span v-else>&#x4FDD;&#x5B58;</span></button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { createCustomer, deleteCustomer, listCustomers, updateCustomer } from '../api/contract'

const monthlyText = '\u6708\u7ed3'
const cashText = '\u73b0\u7ed3'
const mode = ref('list')
const formMode = ref('create')
const editingId = ref(null)
const saving = ref(false)
const customers = ref([])
const demoCustomers = ref(createDemoCustomers())
const detailRow = ref({})
const filters = ref({ keyword: '', customerType: '', cooperationStatus: '', salesManager: '' })
const currentPage = ref(1)
const pageSize = ref(20)
const jumpPage = ref(1)
const sortMode = ref('lastOrderDesc')

const emptyForm = () => ({
  lastOrderYear: '', province: '', city: '', regionName: '', areaName: '', customerCode: '', customerType: '', cooperationStatus: '', salesManager: '', serviceStaff: '',
  phone: '', contact: '', address: '', addressLng: '', addressLat: '', remark: '', yearPositiveAmount: '', yearTotalAmount: '',
  monthPositiveAmount: '', monthTotalAmount: '', paymentMethod: '', logistics: '',
  customerLevel: '', cooperationMode: '', internalDiscount: '', quoteSystem: '', cooperationStartDate: '', enabled: '\u542f\u7528',
  settleType: cashText, settlementMethod: '', creditDays: '', freightPayer: '', invoiceEnabled: '\u5426',
  annualOrderAmount: '', yearPositiveAmountReadonly: '', yearTotalAmountReadonly: '', monthOrderAmount: '', debtAmount: '', lastOrderDate: ''
})
const form = ref(emptyForm())

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 8 }, (_, index) => String(currentYear - index))
const provinceOptions = ['\u5e7f\u4e1c\u7701', '\u6d59\u6c5f\u7701', '\u6c5f\u82cf\u7701', '\u5c71\u4e1c\u7701', '\u6cb3\u5357\u7701', '\u56db\u5ddd\u7701', '\u6e56\u5317\u7701', '\u8fbd\u5b81\u7701', '\u4e0a\u6d77\u5e02', '\u5317\u4eac\u5e02']
const cityMap = {
  '\u5e7f\u4e1c\u7701': ['\u5e7f\u5dde\u5e02', '\u6df1\u5733\u5e02', '\u4f5b\u5c71\u5e02', '\u4e1c\u839e\u5e02'],
  '\u6d59\u6c5f\u7701': ['\u676d\u5dde\u5e02', '\u5b81\u6ce2\u5e02', '\u6e29\u5dde\u5e02', '\u7ecd\u5174\u5e02'],
  '\u6c5f\u82cf\u7701': ['\u5357\u4eac\u5e02', '\u82cf\u5dde\u5e02', '\u65e0\u9521\u5e02', '\u5e38\u5dde\u5e02'],
  '\u5c71\u4e1c\u7701': ['\u6d4e\u5357\u5e02', '\u9752\u5c9b\u5e02', '\u4e34\u6c82\u5e02', '\u6f4d\u574a\u5e02'],
  '\u6cb3\u5357\u7701': ['\u90d1\u5dde\u5e02', '\u6d1b\u9633\u5e02', '\u5357\u9633\u5e02', '\u65b0\u4e61\u5e02'],
  '\u56db\u5ddd\u7701': ['\u6210\u90fd\u5e02', '\u7ef5\u9633\u5e02', '\u5fb7\u9633\u5e02', '\u5b9c\u5bbe\u5e02'],
  '\u6e56\u5317\u7701': ['\u6b66\u6c49\u5e02', '\u5b9c\u660c\u5e02', '\u8944\u9633\u5e02', '\u8346\u5dde\u5e02'],
  '\u8fbd\u5b81\u7701': ['\u6c88\u9633\u5e02', '\u5927\u8fde\u5e02', '\u978d\u5c71\u5e02', '\u8425\u53e3\u5e02'],
  '\u4e0a\u6d77\u5e02': ['\u4e0a\u6d77\u5e02'],
  '\u5317\u4eac\u5e02': ['\u5317\u4eac\u5e02']
}
const staffOptions = ['\u5f20\u7ecf\u7406', '\u674e\u7ecf\u7406', '\u738b\u7ecf\u7406', '\u8d75\u7ecf\u7406']
const serviceOptions = ['\u5f20\u5ba2\u670d', '\u674e\u5ba2\u670d', '\u738b\u5ba2\u670d', '\u8d75\u5ba2\u670d']
const paymentOptions = ['\u8f6c\u8d26', '\u73b0\u91d1', '\u652f\u7968', '\u627f\u5151', '\u5176\u4ed6']
const logisticsOptions = ['\u5ba2\u6237\u81ea\u63d0', '\u5de5\u5382\u53d1\u8d27', '\u7269\u6d41\u4e13\u7ebf', '\u9001\u8d27\u4e0a\u95e8', '\u5ba2\u6237\u6307\u5b9a\u7269\u6d41']
const levelOptions = ['A', 'B', 'C', '\u91cd\u70b9\u5ba2\u6237', '\u666e\u901a\u5ba2\u6237']
const customerTypeOptions = ['\u7ecf\u9500\u5546', '\u95e8\u5e97\u5ba2\u6237', '\u88c5\u4fee\u516c\u53f8', '\u54c1\u724c\u4ee3\u5de5\u5ba2\u6237', '\u8d38\u6613\u5546', '\u5de5\u7a0b\u5ba2\u6237', '\u5176\u4ed6']
const statusOptions = ['\u5408\u4f5c\u4e2d', '\u8bd5\u5355\u5ba2\u6237', '\u6682\u505c\u5408\u4f5c', '\u5df2\u6d41\u5931', '\u98ce\u9669\u5ba2\u6237']
const policyLevelOptions = ['S\u7c7b', 'A\u7c7b', 'B\u7c7b', 'C\u7c7b', '\u98ce\u9669\u5ba2\u6237', '\u91cd\u70b9\u5ba2\u6237']
const cooperationModeOptions = ['OEM', '\u7ecf\u9500\u5408\u4f5c', '\u95e8\u5e97\u4f9b\u8d27', '\u88c5\u4f01\u5408\u4f5c', '\u5de5\u7a0b\u4f9b\u8d27', '\u8d38\u6613\u5408\u4f5c', '\u6563\u5355\u5408\u4f5c']
const quoteSystemOptions = ['\u6807\u51c6\u62a5\u4ef7\u4f53\u7cfb', '\u7ecf\u9500\u5546\u62a5\u4ef7\u4f53\u7cfb', '\u5de5\u7a0b\u62a5\u4ef7\u4f53\u7cfb', 'OEM\u4e13\u5c5e\u62a5\u4ef7\u4f53\u7cfb', '\u5ba2\u6237\u4e13\u5c5e\u62a5\u4ef7\u4f53\u7cfb']
const settlementOptions = ['\u73b0\u7ed3', '\u6708\u7ed3', '\u9884\u4ed8\u6b3e', '\u6b3e\u5230\u53d1\u8d27', '\u5206\u6279\u4ed8\u6b3e']
const creditDayOptions = ['0', '30', '45', '60', '90', '\u7279\u6b8a\u8d26\u671f']
const freightPayerOptions = ['\u5ba2\u6237\u627f\u62c5', '\u5de5\u5382\u627f\u62c5', '\u6309\u8ba2\u5355\u534f\u5546', '\u5305\u542b\u5728\u62a5\u4ef7\u5185']
const cityOptions = computed(() => cityMap[form.value.province] || [])

watch(() => form.value.province, () => {
  if (form.value.city && !cityOptions.value.includes(form.value.city)) form.value.city = ''
})
watch(filters, () => {
  currentPage.value = 1
}, { deep: true })
watch(pageSize, () => {
  currentPage.value = 1
})
watch(sortMode, () => {
  currentPage.value = 1
})

const viewRows = computed(() => (customers.value.length ? customers.value : demoCustomers.value).map(toViewRow))
const filteredCustomers = computed(() => {
  const kw = filters.value.keyword.toLowerCase()
  const rows = viewRows.value.filter((row) => {
    const keywordOk = !kw || [row.regionName, row.customerCode, row.contact, row.phone].some((v) => String(v || '').toLowerCase().includes(kw))
    return keywordOk &&
      (!filters.value.customerType || row.customerType === filters.value.customerType) &&
      (!filters.value.cooperationStatus || row.cooperationStatus === filters.value.cooperationStatus) &&
      (!filters.value.salesManager || row.salesManager === filters.value.salesManager)
  })
  return sortCustomers(rows)
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredCustomers.value.length / pageSize.value)))
watch(totalPages, (value) => {
  if (currentPage.value > value) currentPage.value = value
})
const pagedCustomers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredCustomers.value.slice(start, start + pageSize.value)
})
const pageStart = computed(() => filteredCustomers.value.length ? ((currentPage.value - 1) * pageSize.value) + 1 : 0)
const pageEnd = computed(() => Math.min(currentPage.value * pageSize.value, filteredCustomers.value.length))
const statsCards = computed(() => {
  const rows = viewRows.value
  return [
    { title: '\u5ba2\u6237\u603b\u6570', value: rows.length, desc: '\u6240\u6709\u5408\u4f5c\u5ba2\u6237\u603b\u6570', tone: 'blue' },
    { title: 'A\u7c7b\u91cd\u70b9\u5ba2\u6237', value: rows.filter((row) => ['A\u7c7b', 'S\u7c7b'].includes(row.customerLevel)).length, desc: '\u9ad8\u4ef7\u503c\u6218\u7565\u5408\u4f5c\u5ba2\u6237', tone: 'orange' },
    { title: '\u6708\u7ed3\u5ba2\u6237\u6570', value: rows.filter((row) => String(row.settlementMethod || '').includes('\u6708\u7ed3')).length, desc: '\u91c7\u7528\u6708\u7ed3\u7ed3\u7b97\u7684\u5ba2\u6237', tone: 'green' },
    { title: '\u98ce\u9669\u6b20\u6b3e\u5ba2\u6237', value: rows.filter((row) => Number(row.debtAmount || 0) > 0 || row.cooperationStatus === '\u98ce\u9669\u5ba2\u6237').length, desc: '\u6b20\u6b3e\u8d85\u671f\u6216\u9ad8\u98ce\u9669\u5ba2\u6237', tone: 'red' }
  ]
})
const remarkCount = computed(() => String(form.value.remark || '').length)
const readonlyStats = computed(() => [
  { icon: '\u00a5', label: '\u5e74\u5ea6\u8ba2\u5355\u91d1\u989d', value: moneyText(form.value.annualOrderAmount) },
  { icon: '\u6b63', label: '\u5e74\u5ea6\u6b63\u5355\u91d1\u989d', value: moneyText(form.value.yearPositiveAmountReadonly || form.value.yearPositiveAmount) },
  { icon: '\u603b', label: '\u5e74\u5ea6\u603b\u91d1\u989d', value: moneyText(form.value.yearTotalAmountReadonly || form.value.yearTotalAmount) },
  { icon: '\u6708', label: '\u672c\u6708\u8ba2\u5355\u91d1\u989d', value: moneyText(form.value.monthOrderAmount || form.value.monthTotalAmount) },
  { icon: '\u6b20', label: '\u6b20\u6b3e\u91d1\u989d', value: moneyText(form.value.debtAmount), danger: Number(form.value.debtAmount || 0) > 0 },
  { icon: '\u8fd1', label: '\u6700\u8fd1\u4e0b\u5355\u65f6\u95f4', value: form.value.lastOrderDate || '-' }
])
const managerFilterOptions = computed(() => unique(viewRows.value.map((r) => r.salesManager)))
const detailItems = computed(() => [
  ['\u6700\u540e\u5355\u6240\u5c5e\u5e74\u4efd', detailRow.value.lastOrderYear],
  ['\u5ba2\u6237\u7f16\u7801', detailRow.value.customerCode],
  ['\u5ba2\u6237\u7c7b\u578b', detailRow.value.customerType],
  ['\u5408\u4f5c\u72b6\u6001', detailRow.value.cooperationStatus],
  ['\u7701\u4efd', detailRow.value.province],
  ['\u5e02\u533a', detailRow.value.city],
  ['\u8054\u7cfb\u4eba', detailRow.value.contact],
  ['\u7535\u8bdd', detailRow.value.phone],
  ['\u9500\u552e\u7ecf\u7406', detailRow.value.salesManager],
  ['\u5ba2\u670d\u5458', detailRow.value.serviceStaff],
  ['\u5ba2\u6237\u7b49\u7ea7', detailRow.value.customerLevel],
  ['\u7ed3\u7b97\u65b9\u5f0f', detailRow.value.settlementMethod],
  ['\u7269\u6d41', detailRow.value.logistics],
  ['\u5185\u90e8\u6298\u6263', detailRow.value.internalDiscount ? `${detailRow.value.internalDiscount}%` : ''],
  ['\u5e74\u5ea6\u8ba2\u5355\u91d1\u989d', moneyText(detailRow.value.annualOrderAmount)],
  ['\u6b20\u6b3e\u91d1\u989d', moneyText(detailRow.value.debtAmount)],
  ['\u6700\u8fd1\u4e0b\u5355', detailRow.value.lastOrderDate],
  ['201**\u5e741-12\u6708\u6b63\u5355\u91d1\u989d', detailRow.value.yearPositiveAmount],
  ['201**\u5e741-12\u6708\u603b\u91d1\u989d', detailRow.value.yearTotalAmount],
  ['201**\u5e7412\u6708\u6b63\u5355\u91d1\u989d', detailRow.value.monthPositiveAmount],
  ['201**\u5e7412\u6708\u603b\u91d1\u989d', detailRow.value.monthTotalAmount],
  ['\u5907\u6ce8', detailRow.value.remark]
].map(([label, value]) => ({ label, value })))
const contactSummary = computed(() => [
  { icon: '\u263A', label: '\u8054\u7cfb\u4eba', value: detailRow.value.contact },
  { icon: '\u260E', label: '\u7535\u8bdd', value: maskPhone(detailRow.value.phone) },
  { icon: '\u2605', label: '\u9500\u552e\u7ecf\u7406', value: detailRow.value.salesManager },
  { icon: '\u266B', label: '\u5ba2\u670d\u5458', value: detailRow.value.serviceStaff }
])
const baseInfoItems = computed(() => [
  ['\u6700\u540e\u5355\u6240\u5c5e\u5e74\u4efd', detailRow.value.lastOrderYear],
  ['\u7701\u4efd', detailRow.value.province],
  ['\u5e02\u533a', detailRow.value.city],
  ['\u5ba2\u6237\u7f16\u7801', detailRow.value.customerCode],
  ['\u5ba2\u6237\u7c7b\u578b', detailRow.value.customerType],
  ['\u8054\u7cfb\u4eba', detailRow.value.contact],
  ['\u7535\u8bdd', maskPhone(detailRow.value.phone)],
  ['\u9500\u552e\u7ecf\u7406', detailRow.value.salesManager],
  ['\u5ba2\u670d\u5458', detailRow.value.serviceStaff],
  ['\u8be6\u7ec6\u5730\u5740', detailRow.value.address]
].map(([label, value]) => ({ label, value })))
const cooperationInfoItems = computed(() => [
  { label: '\u5408\u4f5c\u72b6\u6001', value: detailRow.value.cooperationStatus || '\u672a\u8bbe\u7f6e', kind: 'status' },
  { label: '\u5ba2\u6237\u7b49\u7ea7', value: detailRow.value.customerLevel, kind: 'level' },
  { label: '\u5185\u90e8\u6298\u6263', value: detailRow.value.internalDiscount ? `${detailRow.value.internalDiscount}%` : '' },
  { label: '\u7ed3\u7b97\u65b9\u5f0f', value: detailRow.value.settlementMethod },
  { label: '\u4ed8\u6b3e\u65b9\u5f0f', value: detailRow.value.paymentMethod },
  { label: '\u7269\u6d41', value: detailRow.value.logistics }
])
const detailStats = computed(() => [
  { icon: '\u00a5', label: '\u5e74\u5ea6\u8ba2\u5355\u91d1\u989d', value: moneyText(detailRow.value.annualOrderAmount) },
  { icon: '\u6b63', label: '\u5e74\u5ea6\u6b63\u5355\u91d1\u989d', value: moneyText(detailRow.value.yearPositiveAmountReadonly || detailRow.value.yearPositiveAmount) },
  { icon: '\u603b', label: '\u5e74\u5ea6\u603b\u91d1\u989d', value: moneyText(detailRow.value.yearTotalAmountReadonly || detailRow.value.yearTotalAmount) },
  { icon: '\u6708', label: '\u672c\u6708\u8ba2\u5355\u91d1\u989d', value: moneyText(detailRow.value.monthOrderAmount || detailRow.value.monthTotalAmount) },
  { icon: '\u6b20', label: '\u6b20\u6b3e\u91d1\u989d', value: moneyText(detailRow.value.debtAmount), danger: Number(detailRow.value.debtAmount || 0) > 0 },
  { icon: '\u8fd1', label: '\u6700\u8fd1\u4e0b\u5355', value: detailRow.value.lastOrderDate || '\u2014' }
])

function unique(values) {
  return Array.from(new Set(values.filter(Boolean)))
}

function createDemoCustomers() {
  const rows = [
    ['demo-1', 'CUST0001', '\u6d59\u6c5f\u5609\u79be\u5efa\u6750\u6709\u9650\u516c\u53f8', '\u7ecf\u9500\u5546', '\u6d59\u6c5f', '\u676d\u5dde', '\u674e\u660e', '13812345678', '\u5f20\u4f1f', '\u5408\u4f5c\u4e2d', 'A\u7c7b', '\u6708\u7ed360\u5929', '95', 8560000, 256000, '2024-05-20'],
    ['demo-2', 'CUST0002', '\u82cf\u5dde\u4f18\u54c1\u5bb6\u5c45\u95e8\u5e97', '\u95e8\u5e97\u5ba2\u6237', '\u6c5f\u82cf', '\u82cf\u5dde', '\u738b\u82b3', '13922222345', '\u674e\u5a1c', '\u5408\u4f5c\u4e2d', 'B\u7c7b', '\u6708\u7ed330\u5929', '100', 2360000, 0, '2024-05-18'],
    ['demo-3', 'CUST0003', '\u4e0a\u6d77\u7b51\u5bb6\u88c5\u9970\u5de5\u7a0b\u6709\u9650\u516c\u53f8', '\u88c5\u4fee\u516c\u53f8', '\u4e0a\u6d77', '\u4e0a\u6d77', '\u9648\u5f3a', '13766668899', '\u5218\u6d0b', '\u5408\u4f5c\u4e2d', 'A\u7c7b', '\u6708\u7ed360\u5929', '92', 6780000, 318500, '2024-05-16'],
    ['demo-4', 'CUST0004', '\u5e7f\u4e1c\u7f8e\u5c45\u667a\u80fd\u5bb6\u5c45\u6709\u9650\u516c\u53f8', '\u54c1\u724c\u4ee3\u5de5\u5ba2\u6237', '\u5e7f\u4e1c', '\u4f5b\u5c71', '\u9ec4\u5065', '13688886677', '\u5f20\u4f1f', '\u5408\u4f5c\u4e2d', 'S\u7c7b', '\u9884\u4ed8\u6b3e', '96', 15680000, 0, '2024-05-14'],
    ['demo-5', 'CUST0005', '\u5b81\u6ce2\u6d77\u5a01\u56fd\u9645\u8d38\u6613\u6709\u9650\u516c\u53f8', '\u8d38\u6613\u5546', '\u6d59\u6c5f', '\u5b81\u6ce2', '\u8d75\u78ca', '13533334455', '\u738b\u78ca', '\u5408\u4f5c\u4e2d', 'B\u7c7b', '\u6708\u7ed330\u5929', '95', 4120000, 96800, '2024-05-12'],
    ['demo-6', 'CUST0006', '\u6df1\u5733\u5e02\u5b8f\u5efa\u5de5\u7a0b\u6709\u9650\u516c\u53f8', '\u5de5\u7a0b\u5ba2\u6237', '\u5e7f\u4e1c', '\u6df1\u5733', '\u5468\u5efa\u56fd', '13455557788', '\u5218\u6d0b', '\u6682\u505c\u5408\u4f5c', 'C\u7c7b', '\u6708\u7ed360\u5929', '90', 1250000, 522300, '2024-04-28']
  ]
  return rows.map(([id, customerCode, name, customerType, province, city, contact, phone, owner, cooperationStatus, level, settlementMethod, internalDiscount, annualOrderAmount, debtAmount, lastOrderDate]) => ({
    id,
    demo: true,
    customerCode,
    customerName: name,
    phone,
    address: `${province}${city}`,
    owner,
    level,
    createdAt: `${lastOrderDate}T09:00:00`,
    updatedAt: `${lastOrderDate}T09:00:00`,
    customFields: JSON.stringify({ customerCode, customerType, province, city, areaName: city, serviceStaff: '', contact, cooperationStatus, cooperationMode: customerType === '品牌代工客户' ? 'OEM' : '经销合作', quoteSystem: customerType === '品牌代工客户' ? 'OEM专属报价体系' : '标准报价体系', cooperationStartDate: '2024-01-01', enabled: '启用', settlementMethod, creditDays: settlementMethod.includes('60') ? '60' : settlementMethod.includes('30') ? '30' : '0', paymentMethod: '转账', logistics: '物流专线', freightPayer: '按订单协商', invoiceEnabled: '是', internalDiscount, annualOrderAmount, yearPositiveAmountReadonly: Math.round(annualOrderAmount * 0.86), yearTotalAmountReadonly: annualOrderAmount, monthOrderAmount: Math.round(annualOrderAmount / 12), debtAmount, lastOrderDate, customerLevel: level })
  }))
}

function parseCustomFields(value) {
  if (!value) return {}
  if (typeof value === 'object') return value
  try { return JSON.parse(value) } catch { return {} }
}

function dateText(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function toViewRow(row) {
  const extra = parseCustomFields(row.customFields ?? row.custom_fields)
  const regionName = row.customerName || row.customer_name || ''
  return {
    id: row.id,
    raw: row,
    demo: Boolean(row.demo),
    regionName,
    customerCode: row.customerCode || row.customer_code || extra.customerCode || '',
    customerType: extra.customerType || '',
    cooperationStatus: extra.cooperationStatus || '',
    cooperationMode: extra.cooperationMode || '',
    quoteSystem: extra.quoteSystem || '',
    cooperationStartDate: extra.cooperationStartDate || '',
    enabled: extra.enabled || '\u542f\u7528',
    phone: row.phone || '',
    address: row.address || '',
    addressLng: extra.addressLng || '',
    addressLat: extra.addressLat || '',
    salesManager: row.owner || '',
    customerLevel: row.level || extra.customerLevel || '',
    province: extra.province || '',
    city: extra.city || '',
    areaName: extra.areaName || '',
    provinceCity: [extra.province, extra.city].filter(Boolean).join(' / '),
    lastOrderYear: extra.lastOrderYear || '',
    serviceStaff: extra.serviceStaff || '',
    remark: extra.remark || '',
    contact: extra.contact || '',
    yearPositiveAmount: extra.yearPositiveAmount || '',
    yearTotalAmount: extra.yearTotalAmount || '',
    monthPositiveAmount: extra.monthPositiveAmount || '',
    monthTotalAmount: extra.monthTotalAmount || '',
    paymentMethod: extra.paymentMethod || '',
    logistics: extra.logistics || '',
    internalDiscount: extra.internalDiscount || '',
    settlementMethod: extra.settlementMethod || extra.settleType || cashText,
    creditDays: extra.creditDays || '',
    freightPayer: extra.freightPayer || '',
    invoiceEnabled: extra.invoiceEnabled || '\u5426',
    annualOrderAmount: extra.annualOrderAmount || '',
    yearPositiveAmountReadonly: extra.yearPositiveAmountReadonly || extra.yearPositiveAmount || '',
    yearTotalAmountReadonly: extra.yearTotalAmountReadonly || extra.yearTotalAmount || '',
    monthOrderAmount: extra.monthOrderAmount || extra.monthTotalAmount || '',
    debtAmount: extra.debtAmount || '',
    lastOrderDate: extra.lastOrderDate || '',
    settleType: extra.settleType || cashText,
    createdAtText: dateText(row.createdAt || row.created_at),
    updatedAtText: dateText(row.updatedAt || row.updated_at)
  }
}

function applyRowToForm(row) {
  form.value = { ...emptyForm(), ...toViewRow(row.raw || row) }
}

async function loadCustomers() {
  const { data } = await listCustomers(filters.value.keyword)
  if (data.code !== 0) return window.alert(data.message || '\u67e5\u8be2\u5ba2\u6237\u5931\u8d25')
  customers.value = data.data || []
}

function startCreate() {
  formMode.value = 'create'
  editingId.value = null
  form.value = emptyForm()
  mode.value = 'form'
}

function startEdit(row) {
  formMode.value = 'edit'
  editingId.value = row.demo ? null : row.id
  applyRowToForm(row)
  mode.value = 'form'
}

function viewCustomer(row) {
  detailRow.value = row
  mode.value = 'detail'
}

function backToList() {
  mode.value = 'list'
  editingId.value = null
}

function resetFilters() {
  filters.value = { keyword: '', customerType: '', cooperationStatus: '', salesManager: '' }
  currentPage.value = 1
  loadCustomers()
}

function levelClass(level) {
  if (['S\u7c7b', 'A\u7c7b', '\u91cd\u70b9\u5ba2\u6237'].includes(level)) return 'important'
  if (['B\u7c7b', 'C\u7c7b', '\u666e\u901a\u5ba2\u6237'].includes(level)) return 'normal'
  return ''
}

function statusClass(status) {
  if (status === '\u5408\u4f5c\u4e2d') return 'active'
  if (status === '\u8bd5\u5355\u5ba2\u6237') return 'trial'
  if (status === '\u6682\u505c\u5408\u4f5c' || status === '\u5df2\u6d41\u5931') return 'paused'
  if (status === '\u98ce\u9669\u5ba2\u6237') return 'risk'
  return ''
}

function maskPhone(phone) {
  const text = String(phone || '')
  return text.length >= 7 ? `${text.slice(0, 3)}****${text.slice(-4)}` : text
}

function moneyText(value) {
  const num = Number(value || 0)
  return `\u00a5${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function dateValue(value) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

function levelWeight(level) {
  return { 'S\u7c7b': 4, 'A\u7c7b': 3, 'B\u7c7b': 2, 'C\u7c7b': 1, '\u91cd\u70b9\u5ba2\u6237': 3, '\u666e\u901a\u5ba2\u6237': 1 }[level] || 0
}

function statusRiskWeight(status) {
  return { '\u98ce\u9669\u5ba2\u6237': 5, '\u6682\u505c\u5408\u4f5c': 4, '\u5df2\u6d41\u5931': 3, '\u8bd5\u5355\u5ba2\u6237': 2, '\u5408\u4f5c\u4e2d': 1 }[status] || 0
}

function sortCustomers(rows) {
  const sorted = [...rows]
  const byRecentOrder = (a, b) => dateValue(b.lastOrderDate) - dateValue(a.lastOrderDate)
  const sorters = {
    lastOrderDesc: byRecentOrder,
    annualDesc: (a, b) => Number(b.annualOrderAmount || 0) - Number(a.annualOrderAmount || 0) || byRecentOrder(a, b),
    debtDesc: (a, b) => Number(b.debtAmount || 0) - Number(a.debtAmount || 0) || byRecentOrder(a, b),
    statusRisk: (a, b) => statusRiskWeight(b.cooperationStatus) - statusRiskWeight(a.cooperationStatus) || Number(b.debtAmount || 0) - Number(a.debtAmount || 0),
    levelDesc: (a, b) => levelWeight(b.customerLevel) - levelWeight(a.customerLevel) || Number(b.annualOrderAmount || 0) - Number(a.annualOrderAmount || 0),
    nameAsc: (a, b) => String(a.regionName || '').localeCompare(String(b.regionName || ''), 'zh-CN'),
    createdDesc: (a, b) => dateValue(b.createdAtText) - dateValue(a.createdAtText)
  }
  return sorted.sort(sorters[sortMode.value] || byRecentOrder)
}

function goJumpPage() {
  const next = Number(jumpPage.value || 1)
  currentPage.value = Math.min(totalPages.value, Math.max(1, next))
}

function openReconcile(row) {
  window.alert(`\u5bf9\u8d26\u5165\u53e3\uff1a${row.regionName || ''}`)
}

async function copyCustomerCode(code) {
  if (!code) return
  try {
    await navigator.clipboard?.writeText(String(code))
    window.alert('\u5ba2\u6237\u7f16\u7801\u5df2\u590d\u5236')
  } catch {
    window.alert(String(code))
  }
}

function buildPayload() {
  const extra = {
    lastOrderYear: form.value.lastOrderYear,
    customerCode: form.value.customerCode,
    customerType: form.value.customerType,
    cooperationStatus: form.value.cooperationStatus,
    cooperationMode: form.value.cooperationMode,
    quoteSystem: form.value.quoteSystem,
    cooperationStartDate: form.value.cooperationStartDate,
    enabled: form.value.enabled,
    province: form.value.province,
    city: form.value.city,
    addressLng: form.value.addressLng,
    addressLat: form.value.addressLat,
    serviceStaff: form.value.serviceStaff,
    remark: form.value.remark,
    contact: form.value.contact,
    yearPositiveAmount: form.value.yearPositiveAmount,
    yearTotalAmount: form.value.yearTotalAmount,
    monthPositiveAmount: form.value.monthPositiveAmount,
    monthTotalAmount: form.value.monthTotalAmount,
    paymentMethod: form.value.paymentMethod,
    logistics: form.value.logistics,
    customerLevel: form.value.customerLevel,
    internalDiscount: form.value.internalDiscount,
    settlementMethod: form.value.settlementMethod,
    creditDays: form.value.creditDays,
    freightPayer: form.value.freightPayer,
    invoiceEnabled: form.value.invoiceEnabled,
    annualOrderAmount: form.value.annualOrderAmount,
    yearPositiveAmountReadonly: form.value.yearPositiveAmountReadonly || form.value.yearPositiveAmount,
    yearTotalAmountReadonly: form.value.yearTotalAmountReadonly || form.value.yearTotalAmount,
    monthOrderAmount: form.value.monthOrderAmount || form.value.monthTotalAmount,
    debtAmount: form.value.debtAmount,
    lastOrderDate: form.value.lastOrderDate,
    settleType: form.value.settleType
  }
  return {
    customerName: form.value.regionName,
    phone: form.value.phone,
    address: form.value.address,
    level: form.value.customerLevel,
    owner: form.value.salesManager,
    customFields: JSON.stringify(extra)
  }
}

async function saveCustomer() {
  if (!form.value.regionName) return window.alert('\u8bf7\u586b\u5199\u5ba2\u6237\u540d\u79f0')
  saving.value = true
  try {
    const payload = buildPayload()
    const resp = editingId.value ? await updateCustomer(editingId.value, payload) : await createCustomer(payload)
    if (resp.data.code !== 0) return window.alert(resp.data.message || '\u4fdd\u5b58\u5931\u8d25')
    await loadCustomers()
    backToList()
    window.alert('\u4fdd\u5b58\u6210\u529f')
  } catch (e) {
    window.alert(e?.response?.data?.message || e?.message || '\u4fdd\u5b58\u5931\u8d25')
  } finally {
    saving.value = false
  }
}

async function removeCustomer(row) {
  if (!window.confirm('\u786e\u8ba4\u5220\u9664\u8be5\u5ba2\u6237\u6863\u6848\u5417\uff1f')) return
  if (row.demo) {
    demoCustomers.value = demoCustomers.value.filter((item) => item.id !== row.id)
    return
  }
  const { data } = await deleteCustomer(row.id)
  if (data.code !== 0) return window.alert(data.message || '\u5220\u9664\u5931\u8d25')
  await loadCustomers()
  window.alert('\u5220\u9664\u6210\u529f')
}

function exportCustomers() {
  const headers = ['regionName', 'customerCode', 'customerType', 'provinceCity', 'contact', 'phone', 'salesManager', 'cooperationStatus', 'customerLevel', 'settlementMethod', 'internalDiscount', 'annualOrderAmount', 'debtAmount', 'lastOrderDate']
  const csv = [headers.join(','), ...filteredCustomers.value.map((row) => headers.map((key) => `"${String(row[key] || '').replace(/"/g, '""')}"`).join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'customers.csv'
  link.click()
  URL.revokeObjectURL(link.href)
}

onMounted(loadCustomers)
</script>

<style scoped>
.customer-center { display: grid; gap: 14px; max-width: 1720px; margin: 0 auto; color: #13213b; text-align: left; }
.toolbar-card, .list-card, .detail-card, .form-card, .form-actions, .stat-card, .subpage-actions, .customer-overview-card, .detail-action-bar, .profile-hero-card, .detail-info-card, .remark-card { background: #fff; border: 1px solid #e4ecf7; border-radius: 12px; box-shadow: 0 8px 24px rgba(20,42,82,.06); }
h2, h3, h4 { margin: 0; color: #071631; }
h2 { font-size: 24px; }
.row-actions, .form-actions, .toolbar-actions, .subpage-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.toolbar-card { padding: 14px; display: grid; grid-template-columns: minmax(320px, 1.6fr) repeat(3, minmax(150px, .7fr)) auto; gap: 10px; align-items: center; }
.toolbar-actions { justify-content: flex-end; white-space: nowrap; }
.subpage-actions { justify-content: space-between; padding: 12px 16px; }
.detail-action-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 18px; }
.subpage-title h3 { font-size: 18px; }
.subpage-title p { margin: 6px 0 0; color: #68768d; font-size: 13px; }
.customer-overview-card { display: flex; align-items: center; gap: 14px; padding: 16px 18px; }
.company-avatar { width: 48px; height: 48px; border-radius: 14px; display: grid; place-items: center; background: #eef4ff; color: #2f69ef; font-size: 20px; font-weight: 900; }
.overview-main { display: grid; gap: 4px; min-width: 0; flex: 1; }
.overview-main strong { color: #071631; font-size: 18px; }
.overview-main span { color: #68768d; font-size: 13px; }
input, select, textarea { width: 100%; min-width: 0; box-sizing: border-box; border: 1px solid #d7e0ef; border-radius: 8px; background: #fff; color: #10203c; outline: 0; }
input, select { height: 42px; padding: 0 12px; }
textarea { min-height: 86px; resize: vertical; padding: 12px; line-height: 1.5; }
input:focus, select:focus, textarea:focus { border-color: #3b74ff; box-shadow: 0 0 0 3px rgba(59,116,255,.12); }
.readonly-input { background: #f5f7fb; color: #64748b; cursor: not-allowed; }
.primary-btn, .ghost-btn, .mini-btn { border-radius: 8px; font-weight: 700; }
.primary-btn, .ghost-btn { height: 42px; padding: 0 18px; }
.primary-btn { background: linear-gradient(90deg,#2f69ef,#4b7df3); color: #fff; box-shadow: 0 8px 16px rgba(47,105,239,.22); }
.ghost-btn { background: #f8fafd; color: #40516d; border: 1px solid #dbe4f2; }
.ghost-btn:disabled { opacity: .5; cursor: not-allowed; }
.mini-btn { height: 30px; padding: 0 10px; background: #eef4ff; color: #2353b8; }
.mini-btn.danger { background: #fff1f2; color: #be123c; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.stat-card { min-height: 94px; padding: 16px; display: flex; align-items: flex-start; gap: 12px; }
.stat-mark { width: 10px; height: 42px; border-radius: 999px; background: #2f69ef; flex: 0 0 auto; }
.stat-mark.orange { background: #f59e0b; }
.stat-mark.green { background: #10b981; }
.stat-mark.purple { background: #8b5cf6; }
.stat-mark.red { background: #ef4444; }
.stat-card p { margin: 0; color: #68768d; font-size: 13px; }
.stat-card strong { display: block; margin-top: 8px; color: #071631; font-size: 28px; line-height: 1; }
.stat-card small { display: block; margin-top: 8px; color: #8a98ad; font-size: 12px; }
.list-card { padding: 16px; }
.list-summary { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #68768d; margin-bottom: 12px; }
.list-summary strong { color: #2353b8; font-size: 22px; }
.sort-control { display: inline-flex; align-items: center; gap: 8px; color: #68768d; font-size: 13px; }
.sort-control select { width: 220px; height: 34px; border-radius: 8px; font-size: 13px; }
.table-wrap { overflow: auto; }
table { width: 100%; min-width: 1540px; border-collapse: separate; border-spacing: 0; font-size: 13px; }
th { background: #f3f7fc; color: #2d3d58; text-align: left; padding: 12px; border-bottom: 1px solid #e2e8f0; }
td { padding: 12px; border-bottom: 1px solid #eef2f7; vertical-align: middle; }
tbody tr { cursor: pointer; }
tbody tr:hover { background: #f8fbff; }
td strong { display: block; color: #10203c; }
td small { display: block; margin-top: 4px; color: #7a879d; }
.address-cell { max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.level-pill, .settle-pill, .status-pill { display: inline-flex; min-width: 32px; justify-content: center; padding: 3px 9px; border-radius: 999px; background: #eef4ff; color: #2353b8; white-space: nowrap; }
.level-pill.important { background: #fff7ed; color: #c2410c; }
.level-pill.normal { background: #f1f5f9; color: #475569; }
.settle-pill { background: #f0fdf4; color: #15803d; }
.status-pill.active { background: #ecfdf5; color: #047857; }
.status-pill.trial { background: #fff7ed; color: #c2410c; }
.status-pill.paused { background: #f1f5f9; color: #64748b; }
.status-pill.risk { background: #fef2f2; color: #dc2626; }
.money-head, .money-cell { text-align: right; }
.money-cell { font-variant-numeric: tabular-nums; white-space: nowrap; }
.money-cell.overdue { color: #dc2626; font-weight: 800; }
.pagination { display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding-top: 14px; color: #68768d; font-size: 13px; }
.pagination select { width: 112px; }
.jump-input { width: 72px; }
.page-btn { min-width: 82px; padding: 0 12px; }
.page-index { min-width: 56px; text-align: center; color: #10203c; font-weight: 700; }
.empty-state { min-height: 360px; display: grid; place-items: center; align-content: center; gap: 12px; text-align: center; color: #68768d; }
.empty-illu { width: 72px; height: 72px; display: grid; place-items: center; border-radius: 20px; background: #eef4ff; color: #2f69ef; font-size: 30px; font-weight: 800; }
.profile-hero-card { padding: 28px; display: grid; gap: 22px; }
.profile-hero-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; }
.profile-company { display: flex; align-items: flex-start; gap: 16px; min-width: 0; flex: 1; }
.profile-avatar { width: 76px; height: 76px; border-radius: 50%; display: grid; place-items: center; background: linear-gradient(135deg, #e8f0ff, #f4f8ff); color: #2f69ef; font-size: 32px; font-weight: 900; flex: 0 0 auto; box-shadow: inset 0 0 0 1px #dbe7ff; }
.profile-title { min-width: 0; }
.profile-title h3 { font-size: 30px; line-height: 1.22; margin: 0; color: #071631; font-weight: 900; }
.profile-address { margin: 8px 0 0; color: #64748b; font-size: 14px; }
.code-badge-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-top: 13px; }
.code-chip { height: 32px; display: inline-flex; align-items: center; gap: 8px; border-radius: 999px; padding: 0 13px; background: #fbfdff; color: #475569; border: 1px solid #dbe4f2; font-weight: 800; }
.profile-badges { display: flex; justify-content: flex-start; gap: 8px; flex-wrap: wrap; }
.type-badge { display: inline-flex; align-items: center; min-height: 28px; padding: 3px 11px; border-radius: 999px; background: #f8fafc; color: #334155; font-weight: 800; white-space: nowrap; }
.type-badge.type-blue { background: #eff6ff; color: #1d4ed8; }
.hero-remark { width: min(420px, 34vw); min-height: 116px; padding: 14px 16px; border-radius: 14px; background: #fbfdff; border: 1px solid #e6eef9; }
.hero-remark .detail-card-head { margin-bottom: 8px; }
.hero-remark .detail-card-head h3 { font-size: 15px; }
.hero-remark p { margin: 0; color: #334155; line-height: 1.6; font-size: 13px; white-space: pre-wrap; }
.contact-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; padding-top: 16px; border-top: 1px solid #eef2f7; }
.contact-summary article { display: flex; align-items: center; gap: 12px; min-height: 78px; padding: 15px; border-radius: 12px; background: #fbfdff; border: 1px solid #e6eef9; }
.summary-icon { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; background: #eef4ff; color: #2353b8; font-weight: 900; flex: 0 0 auto; }
.contact-summary p { margin: 0 0 5px; color: #71809a; font-size: 12px; }
.contact-summary strong { color: #10203c; font-size: 15px; font-weight: 850; }
.detail-columns { display: grid; grid-template-columns: minmax(0, .95fr) minmax(0, .85fr) minmax(420px, 1.2fr); gap: 14px; align-items: stretch; }
.detail-info-card, .remark-card { padding: 22px; }
.detail-info-card { height: 100%; box-sizing: border-box; }
.detail-info-card h3, .remark-card h3 { font-size: 18px; margin: 0; display: inline-flex; align-items: center; gap: 8px; }
.card-title-icon { width: 28px; height: 28px; display: inline-grid; place-items: center; border-radius: 9px; background: #eef4ff; color: #2353b8; font-size: 13px; }
.info-list { display: grid; margin-top: 14px; }
.info-list p { display: grid; grid-template-columns: 118px minmax(0, 1fr); gap: 12px; margin: 0; padding: 12px 0; border-bottom: 1px solid #f0f3f8; }
.info-list p:last-child { border-bottom: 0; }
.info-list span { color: #71809a; font-size: 13px; }
.info-list strong { color: #10203c; font-size: 14px; font-weight: 750; word-break: break-word; }
.state-dot { width: 8px; height: 8px; display: inline-block; margin-right: 7px; border-radius: 999px; background: #94a3b8; }
.state-dot.active { background: #10b981; }
.state-dot.trial { background: #f59e0b; }
.state-dot.paused { background: #94a3b8; }
.state-dot.risk { background: #ef4444; }
.inline-badge { display: inline-flex; padding: 3px 9px; border-radius: 999px; background: #eef4ff; color: #2353b8; font-style: normal; }
.detail-card-head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.detail-card-head span { color: #71809a; font-size: 12px; }
.mini-stat-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.mini-stat { min-height: 112px; padding: 15px; border-radius: 14px; background: linear-gradient(180deg, #fbfdff, #f7faff); border: 1px solid #e2eaf6; display: grid; gap: 8px; align-content: center; }
.mini-stat > span { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 11px; background: #eef4ff; color: #2353b8; font-weight: 900; }
.mini-stat p { margin: 0; color: #71809a; font-size: 12px; }
.mini-stat strong { color: #10203c; font-size: 18px; font-variant-numeric: tabular-nums; font-weight: 900; }
.mini-stat.danger { background: #fff7f7; border-color: #fecaca; }
.mini-stat.danger > span { background: #fee2e2; color: #dc2626; }
.mini-stat.danger strong { color: #dc2626; }
.remark-card { min-height: 118px; }
.remark-card p { margin: 0; color: #334155; line-height: 1.7; white-space: pre-wrap; padding: 2px 0 0; }
.detail-card, .form-card { padding: 24px; }
.detail-title { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.detail-title p { margin: 8px 0 0; color: #68768d; }
.detail-grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 12px; }
.detail-item { min-height: 68px; padding: 12px; border-radius: 8px; background: #f8fbff; border: 1px solid #e6eef9; display: grid; align-content: center; gap: 6px; }
.detail-item span { color: #71809a; font-size: 12px; }
.detail-item strong { color: #10203c; font-size: 14px; }
.form-page { display: grid; gap: 14px; }
.form-card h3 { margin-bottom: 18px; font-size: 18px; }
.form-grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 16px; }
.field-cell { min-width: 0; display: grid; gap: 8px; }
.field-cell span { color: #1d2d4b; font-size: 13px; font-weight: 750; }
.field-cell.required > span::after { content: " *"; color: #dc2626; }
.field-hint { color: #64748b; font-size: 12px; line-height: 1.4; }
.address-textarea { min-height: 112px; }
.span-2 { grid-column: span 2; }
.span-4 { grid-column: 1 / -1; }
.card-title-row { display: flex; align-items: baseline; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.card-title-row h3 { margin-bottom: 0; }
.card-title-row span { color: #71809a; font-size: 12px; }
.readonly-card { background: #fbfcff; }
.readonly-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.readonly-stat { display: flex; align-items: center; gap: 12px; min-height: 78px; padding: 14px; border-radius: 10px; background: #f8fbff; border: 1px solid #e6eef9; }
.readonly-stat.danger { background: #fff7f7; border-color: #fecaca; }
.readonly-icon { width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center; background: #eef4ff; color: #2353b8; font-weight: 900; }
.readonly-stat.danger .readonly-icon { background: #fee2e2; color: #dc2626; }
.readonly-stat p { margin: 0 0 6px; color: #71809a; font-size: 12px; }
.readonly-stat strong { color: #10203c; font-size: 17px; font-variant-numeric: tabular-nums; }
.readonly-stat.danger strong { color: #dc2626; }
.amount-groups { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 16px; }
.amount-group { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 12px; padding: 16px; border-radius: 8px; background: #f8fbff; border: 1px solid #e6eef9; }
.amount-group h4 { grid-column: 1 / -1; font-size: 15px; }
.business-grid { grid-template-columns: repeat(5, minmax(0,1fr)); }
.radio-row { height: 42px; display: flex; align-items: center; gap: 18px; }
.radio-row label { display: inline-flex; align-items: center; gap: 6px; }
.radio-row input { width: 16px; height: 16px; }
.form-actions { justify-content: flex-end; padding: 14px 18px; position: sticky; bottom: 10px; z-index: 2; }
@media (max-width: 1320px) {
  .toolbar-card, .form-grid, .business-grid, .detail-grid, .readonly-stats { grid-template-columns: repeat(2, minmax(0,1fr)); }
  .toolbar-actions { justify-content: flex-start; }
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .detail-columns { grid-template-columns: 1fr; }
  .hero-remark { width: 100%; }
  .contact-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .amount-groups { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .detail-title, .subpage-actions, .customer-overview-card, .card-title-row, .detail-action-bar, .profile-hero-top, .profile-company, .code-badge-row { flex-direction: column; align-items: stretch; }
  .toolbar-card, .form-grid, .business-grid, .detail-grid, .amount-group, .stats-grid, .readonly-stats { grid-template-columns: 1fr; }
  .contact-summary, .mini-stat-grid { grid-template-columns: 1fr; }
  .info-list p { grid-template-columns: 1fr; gap: 4px; }
  .span-2, .span-4 { grid-column: auto; }
  .pagination { justify-content: flex-start; flex-wrap: wrap; }
  .list-summary { align-items: flex-start; flex-direction: column; }
  .sort-control, .sort-control select { width: 100%; }
}
</style>
