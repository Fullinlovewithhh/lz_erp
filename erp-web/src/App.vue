<template>
  <div v-if="!isLoggedIn" class="login-page">
    <div class="login-card">
      <div class="login-visual" :style="{ backgroundImage: `url(${loginBg})` }">
      </div>
      <div class="login-form-wrap">
        <img :src="brandLogo" alt="LONZVINE" class="login-right-logo" />
        <div class="login-title">龙泽伟尼信息系统</div>
        <div class="login-sub">账号密码登录</div>
        <div class="login-form">
          <label class="login-input">
            <span class="login-input-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="8" r="3.5" stroke="currentColor" stroke-width="1.8"/>
                <path d="M5 19.5c1.7-3 4.2-4.5 7-4.5s5.3 1.5 7 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
            </span>
            <input v-model.trim="loginForm.username" placeholder="账号（姓名 / 工号）" @keyup.enter="doLogin" />
          </label>
          <label class="login-input">
            <span class="login-input-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none">
                <rect x="5" y="10" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.8"/>
                <path d="M8 10V7.8A4 4 0 0 1 12 4a4 4 0 0 1 4 3.8V10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
            </span>
            <input v-model="loginForm.password" type="password" placeholder="密码" @keyup.enter="doLogin" />
          </label>
          <button class="primary login-btn" @click="doLogin">登录系统</button>
          <div class="login-safe-note">安全登录 · 数据加密 · 权限保护</div>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="erp-layout" :class="{ collapsed: sidebarCollapsed }">
    <aside class="left-nav" :class="{ collapsed: sidebarCollapsed }">
      <div class="brand">
        <img :src="brandLogo" alt="LONZVINE" class="brand-logo" :class="{ 'is-collapsed': sidebarCollapsed }" />
        <div v-if="!sidebarCollapsed" class="brand-text">
          <div class="brand-title">全屋定制ERP</div>
          <div class="brand-sub">工厂运营平台</div>
        </div>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">{{ sidebarCollapsed ? '›' : '‹' }}</button>
      </div>

      <div class="menu-search" v-if="!sidebarCollapsed">
        <input v-model="menuKeyword" placeholder="搜索菜单..." />
      </div>

      <div class="menu-group" v-for="module in filteredModules" :key="module.name">
        <button class="module-btn" :class="{ active: activeModule === module.name, icononly: sidebarCollapsed, disabled: !moduleAllowed(module.name) }" @click="switchModule(module.name)" :title="moduleAllowed(module.name) ? moduleDisplayName(module.name) : `${moduleDisplayName(module.name)}（无权限）`">
          <span class="module-btn-main">
            <span class="module-icon">{{ menuIcon(module.name) }}</span>
            <span v-if="!sidebarCollapsed" class="module-name">{{ moduleDisplayName(module.name) }}</span>
          </span>
          <span class="badge" v-if="!sidebarCollapsed">{{ module.children.length }}</span>
          <span class="badge-dot" v-else-if="module.children.length > 0">{{ module.children.length }}</span>
        </button>
        <div class="submenu" v-if="!sidebarCollapsed && activeModule === module.name">
          <button
            v-for="sub in module.children"
            :key="sub"
            class="sub-btn"
            :class="{ active: activeSubModule === subName(sub) }"
            @click="switchSubModule(subName(sub))"
          >
            {{ sub }}
          </button>
        </div>
      </div>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div class="topbar-left">
          <h1>{{ activeSubTitle }}</h1>
          <p v-if="!isCustomerArchivePage" class="crumb">{{ moduleDisplayName(activeModule) }} / {{ activeSubTitle }}</p>
        </div>
        <div class="top-search" v-if="!isCustomerArchivePage">
          <input
            v-model.trim="globalSearch"
            placeholder="全局搜索工厂订单号/客户订单号/客户/关键词..."
            @keyup.enter="applyGlobalSearch"
          />
        </div>
        <div class="actions">
          <span class="login-user">当前用户：{{ loginUserName }}</span>
          <button class="ghost" @click="logout">退出登录</button>
          <button class="ghost" v-if="showFilterBtn" @click="openFilter">筛选</button>
          <button class="ghost" v-if="showImportBtn" @click="openImport">批量导入</button>
          <button class="ghost" v-if="showTemplateBtn" @click="downloadTemplate">模板下载</button>
          <button class="ghost" v-if="showExportBtn" @click="exportCurrent">导出</button>
          <button class="ghost" v-if="isBatchPage && !batchMode" @click="enterBatchMode">批量</button>
          <button class="ghost" v-if="isBatchPage && batchMode" @click="batchSetStatus('启用')">批量启用</button>
          <button class="ghost" v-if="isBatchPage && batchMode" @click="batchSetStatus('弃用')">批量弃用</button>
          <button class="ghost" v-if="isBatchPage && batchMode" @click="batchSetStatus('暂存')">批量暂存</button>
          <button class="ghost danger" v-if="isBatchPage && batchMode" @click="batchDelete">批量删除</button>
          <button class="ghost" v-if="isBatchPage && batchMode" @click="exitBatchMode">取消批量</button>
          <button class="primary" v-if="showAddBtn" @click="openAdd">+ 新增</button>
        </div>
      </header>

      <section class="card" v-if="activeModule === C.master && activeSubModule === C.staff">
        <div class="table-tools">
          <input v-model="masterSearch" placeholder="搜索人员/角色/工序" />
          <button @click="refreshMasterData">查询</button>
        </div>
        <table>
          <thead><tr><th class="batch-col" v-if="batchMode"><input type="checkbox" :checked="isAllSelected(C.staff, staffRows)" @change="toggleSelectAll(C.staff, staffRows, $event)" /></th><th>人员编码</th><th>姓名</th><th>角色</th><th>工序</th><th>电话</th><th>状态</th><th>备注</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in staffRows" :key="row.id">
              <td class="batch-col" v-if="batchMode"><input type="checkbox" :checked="isRowSelected(C.staff, row.id)" @change="toggleRowSelect(C.staff, row.id, $event)" /></td>
              <td>{{ row.staff_code }}</td><td>{{ row.staff_name }}</td><td>{{ row.role_type }}</td><td>{{ row.process_name }}</td><td>{{ row.phone }}</td><td><span class="status-pill" :class="statusClass(row.status)">{{ row.status || '暂存' }}</span></td><td>{{ row.custom_fields }}</td>
              <td>
                <button class="mini" title="详情" @click="viewDetail(C.staff, row.id)">详情</button>
                <button class="mini" title="编辑" @click="openEdit(C.staff, row)">编辑</button>
                <button class="mini danger" title="删除" @click="removeRow(C.staff, row.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.master && activeSubModule === C.userAuth">
        <div class="table-tools">
          <input v-model="authUserKeyword" placeholder="搜索账号/姓名/角色" />
          <button @click="loadAuthUsers">查询</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>ID</th><th>账号</th><th>姓名</th><th>角色</th><th>状态</th><th>创建时间</th><th>更新时间</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in authUsers" :key="u.id">
              <td>{{ u.id }}</td>
              <td>{{ u.username }}</td>
              <td>{{ u.display_name }}</td>
              <td>{{ u.role_code }}</td>
              <td><span class="status-pill" :class="Number(u.enabled) === 1 ? 'status-enabled' : 'status-disabled'">{{ Number(u.enabled) === 1 ? '启用' : '禁用' }}</span></td>
              <td>{{ formatDateTime(u.created_at) }}</td>
              <td>{{ formatDateTime(u.updated_at) }}</td>
              <td>
                <button class="mini" @click="openAuthUserEdit(u)">编辑</button>
                <button class="mini" @click="resetAuthPwd(u)">重置密码</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.master && activeSubModule === C.product">
        <div class="table-tools">
          <input v-model="masterSearch" placeholder="搜索产品风格/产品名称/颜色" />
          <button @click="refreshMasterData">查询</button>
        </div>
        <table>
          <thead><tr><th class="batch-col" v-if="batchMode"><input type="checkbox" :checked="isAllSelected(C.product, productRows)" @change="toggleSelectAll(C.product, productRows, $event)" /></th><th>产品编码</th><th>产品类型</th><th>产品风格</th><th>产品名称</th><th>材质</th><th>颜色</th><th>拉手颜色</th><th>单价</th><th>单价单位</th><th>厚度</th><th>厚度单位</th><th>尺寸</th><th>状态</th><th>照片</th><th>更新时间</th><th>备注</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in productRows" :key="row.id">
              <td class="batch-col" v-if="batchMode"><input type="checkbox" :checked="isRowSelected(C.product, row.id)" @change="toggleRowSelect(C.product, row.id, $event)" /></td>
              <td>{{ row.product_code }}</td><td>{{ row.type }}</td><td>{{ row.product_name }}</td><td>{{ row.model }}</td><td>{{ row.material_name }}</td><td>{{ row.color }}</td><td>{{ displayEmpty(row.handle_color) }}</td><td>{{ row.unit_price }}</td><td>{{ row.unit_price_unit || 'm2' }}</td><td>{{ row.thickness }}</td><td>{{ row.thickness_unit || 'mm' }}</td><td>{{ row.size }}</td><td><span class="status-pill" :class="statusClass(row.status)">{{ row.status || '暂存' }}</span></td>
              <td>
                <div class="img-cell drop-paste-zone" tabindex="0" @paste="onPasteRowImage('product', row.id, $event)" @dragover.prevent @drop.prevent="onDropRowImage('product', row.id, $event)">
                  <img v-if="row.image_url" :src="toAbsUrl(row.image_url)" alt="产品图" class="thumb previewable" @click="openImagePreview(toAbsUrl(row.image_url))" @error="onImageError" />
                  <div v-else class="thumb empty-thumb">暂无</div>
                  <span class="upload-tip">支持拖拽/粘贴</span>
                  <label class="mini-upload">上传
                    <input type="file" accept="image/*" @change="onProductFileChange(row.id, $event)" />
                  </label>
                  <button v-if="row.image_url" class="mini danger" title="删除" @click="removeRowImage(C.product, row.id)">删图</button>
                </div>
              </td>
              <td>{{ formatDateTime(row.updated_at) }}</td>
              <td>{{ row.custom_fields }}</td>
              <td>
                <button class="mini" title="详情" @click="viewDetail(C.product, row.id)">详情</button>
                <button class="mini" title="编辑" @click="openEdit(C.product, row)">编辑</button>
                <button class="mini danger" title="删除" @click="removeRow(C.product, row.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.master && activeSubModule === C.materialData">
        <div class="table-tools">
          <input v-model="masterSearch" placeholder="搜索材料/颜色/尺寸" />
          <button @click="refreshMasterData">查询</button>
        </div>
        <table>
          <thead><tr><th class="batch-col" v-if="batchMode"><input type="checkbox" :checked="isAllSelected(C.materialData, materialRows)" @change="toggleSelectAll(C.materialData, materialRows, $event)" /></th><th>材料编码</th><th>材料名称</th><th>材质类型</th><th>颜色</th><th>长mm</th><th>宽mm</th><th>厚mm</th><th>状态</th><th>照片</th><th>更新时间</th><th>备注</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in materialRows" :key="row.id">
              <td class="batch-col" v-if="batchMode"><input type="checkbox" :checked="isRowSelected(C.materialData, row.id)" @change="toggleRowSelect(C.materialData, row.id, $event)" /></td>
              <td>{{ row.material_code }}</td><td>{{ row.material_name }}</td><td>{{ row.material_type }}</td><td>{{ row.color }}</td><td>{{ row.length_mm }}</td><td>{{ row.width_mm }}</td><td>{{ row.thickness_mm }}</td><td><span class="status-pill" :class="statusClass(row.status)">{{ row.status || '暂存' }}</span></td>
              <td>
                <div class="img-cell drop-paste-zone" tabindex="0" @paste="onPasteRowImage('material', row.id, $event)" @dragover.prevent @drop.prevent="onDropRowImage('material', row.id, $event)">
                  <img v-if="row.image_url" :src="toAbsUrl(row.image_url)" alt="材料图" class="thumb previewable" @click="openImagePreview(toAbsUrl(row.image_url))" @error="onImageError" />
                  <div v-else class="thumb empty-thumb">暂无</div>
                  <span class="upload-tip">支持拖拽/粘贴</span>
                  <label class="mini-upload">上传
                    <input type="file" accept="image/*" @change="onMaterialFileChange(row.id, $event)" />
                  </label>
                  <button v-if="row.image_url" class="mini danger" title="删除" @click="removeRowImage(C.materialData, row.id)">删图</button>
                </div>
              </td>
              <td>{{ formatDateTime(row.updated_at) }}</td>
              <td>{{ row.custom_fields }}</td>
              <td>
                <button class="mini" title="详情" @click="viewDetail(C.materialData, row.id)">详情</button>
                <button class="mini" title="编辑" @click="openEdit(C.materialData, row)">编辑</button>
                <button class="mini danger" title="删除" @click="removeRow(C.materialData, row.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.master && activeSubModule === C.processRule">
        <div class="table-tools"><button @click="showProcessRuleNotice">新增工艺规则</button></div>
        <table>
          <thead>
            <tr>
              <th>规则编码</th><th>规则名称</th><th>规则类型</th><th>规则内容</th><th>状态</th><th>备注</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>GY0001</td>
              <td>封边工艺</td>
              <td>生产工艺</td>
              <td>用于记录生产工艺要求，不参与报价计算</td>
              <td><span class="status-pill status-draft">暂存</span></td>
              <td>报价加价请在价格规则中维护</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.master && activeSubModule === C.priceRule">
        <div class="table-tools">
          <div class="field-box compact">
            <label>规则检索</label>
            <input v-model="priceRuleKeyword" placeholder="规则名称/备注" @input="loadPriceRules" />
          </div>
          <button @click="loadPriceRules">查询</button>
        </div>
        <div class="form-grid labeled-grid">
          <div class="field-box"><label>规则大类</label><input v-model="newPriceRule.ruleCategory" list="quoteCategoryList" placeholder="如：柜门" /></div>
          <div class="field-box"><label>规则名称</label><input v-model="newPriceRule.ruleName" placeholder="如：非标颜色" /></div>
          <div class="field-box"><label>计价模式</label><select v-model="newPriceRule.adjustMode">
            <option value="FIXED_PER_M2">按平米</option>
            <option value="PERCENT">按百分比</option>
            <option value="FIXED_PER_ITEM">按件</option>
          </select></div>
          <div class="field-box"><label>增减值</label><input v-model.number="newPriceRule.adjustValue" type="number" placeholder="可负值" /></div>
          <div class="field-box"><label>单位说明</label><input v-model="newPriceRule.unitDesc" placeholder="元/平、%、元/块" /></div>
          <div class="field-box"><label>最小计费面积</label><input v-model.number="newPriceRule.minAreaM2" type="number" step="0.0001" placeholder="可选" /></div>
          <div class="field-box"><label>最低费用</label><input v-model.number="newPriceRule.minCharge" type="number" placeholder="可选" /></div>
          <div class="field-box"><label>最高费用</label><input v-model.number="newPriceRule.maxCharge" type="number" placeholder="可选" /></div>
          <div class="field-box"><label>状态</label><select v-model="newPriceRule.status"><option v-for="s in ruleStatusOptions" :key="s" :value="s">{{ s }}</option></select></div>
          <div class="field-box"><label>备注</label><input v-model="newPriceRule.remark" placeholder="规则备注" /></div>
          <button class="primary" @click="savePriceRule">新增规则</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>规则编码</th><th>规则大类</th><th>规则名称</th><th>模式</th><th>增减值</th><th>单位</th><th>最小面积</th><th>最低费用</th><th>最高费用</th><th>备注</th><th>状态</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in priceRuleRows" :key="r.id">
              <td>{{ r.rule_code }}</td>
              <td>{{ r.rule_category || '-' }}</td>
              <td>{{ r.rule_name }}</td>
              <td>{{ ruleModeText(r.adjust_mode) }}</td>
              <td>{{ r.adjust_value }}</td>
              <td>{{ r.unit_desc || '-' }}</td>
              <td>{{ r.min_area_m2 ?? '-' }}</td>
              <td>{{ r.min_charge ?? '-' }}</td>
              <td>{{ r.max_charge ?? '-' }}</td>
              <td>{{ r.remark || '-' }}</td>
              <td>
                <select class="status-select" :class="ruleStatusClass(r.enabled)" :value="ruleStatusText(r.enabled)" @change="quickRuleStatus(r, $event.target.value)">
                  <option v-for="s in ruleStatusOptions" :key="s" :value="s">{{ s }}</option>
                </select>
              </td>
              <td>
                <button class="mini" @click="viewRuleDetail(r)">查看</button>
                <button class="mini" @click="openRuleEdit(r)">编辑</button>
                <button class="mini danger" @click="removeRule(r)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.order && activeSubModule === '项目订单'">
        <div class="contract-layout">
          <div class="contract-card contract-op-card">
            <div class="contract-op-grid">
              <div class="project-order-title">
                <strong>项目订单</strong>
                <span>按客户项目归集，工厂内部继续以工厂订单号执行。</span>
              </div>
              <div class="contract-op-actions">
                <button class="primary" @click="openFactoryOrderForm">+ 新建工厂订单</button>
                <button @click="loadOrders">刷新</button>
                <button @click="exportCurrent">导出</button>
              </div>
            </div>
          </div>

          <div class="contract-card contract-filter-card">
            <div class="contract-list-tools">
              <input v-model="orderKeyword" placeholder="搜索工厂订单号 / 客户订单号 / 客户名称 / 客户项目号 / 项目名称" />
              <label class="contract-pending-toggle">
                <input type="checkbox" v-model="orderPendingOnly" />
                <span>仅显示待完善合同</span>
              </label>
            </div>
          </div>

          <div class="contract-card contract-main-card" v-if="orderCreateStep === 2">
            <div class="section-title">基础归属信息</div>
            <div class="contract-form-grid">
              <div class="form-item">
                <label>客户名称 <span class="required-star">*</span></label>
                <div class="customer-picker">
                  <input v-model.trim="customerKeyword" placeholder="搜索客户名称/客户编码/手机号" @focus="openCustomerDropdown('order')" @input="onCustomerKeywordInput" @blur="deferCloseCustomerDropdown('order')" />
                  <div v-if="customerDropdownOpen && customerOptions.length" class="customer-dropdown">
                    <button v-for="c in customerOptions" :key="c.id" type="button" @mousedown.prevent="selectOrderCustomer(c)">
                      <strong>{{ customerOptionText(c) }}</strong>
                    </button>
                  </div>
                </div>
              </div>
              <div class="form-item project-select-field">
                <label>项目名称 <span class="required-star">*</span></label>
                <div class="inline-field">
                  <select v-model="orderForm.projectId" @change="onOrderProjectChange">
                    <option value="">{{ orderForm.customerId ? '请选择项目' : '请先选择客户' }}</option>
                    <option v-if="orderForm.customerId" value="__NO_PROJECT__">无具体项目</option>
                    <option v-for="p in customerProjects" :key="p.projectId" :value="String(p.projectId)">{{ p.projectName }}</option>
                  </select>
                  <button class="mini" :disabled="!orderForm.customerId" @click="showInlineProjectCreate">旁边填写项目</button>
                </div>
                <small v-if="orderForm.customerId && !customerProjects.length" class="hint">该客户暂无项目，可选择“无具体项目”或在旁边填写项目</small>
              </div>
              <div class="form-item"><label>客户项目号</label><input :value="selectedOrderProject?.projectNo || ''" placeholder="选择项目后自动带出" disabled /></div>
              <div class="form-item project-select-field">
                <label>客户订单号 <span class="required-star">*</span></label>
                <input v-model.trim="orderForm.customerOrderNo" list="orderCustomerOrderNoList" :disabled="!selectedOrderProject" placeholder="输入客户订单号；已有编号会自动匹配" @input="onOrderCustomerOrderNoInput" />
                <datalist id="orderCustomerOrderNoList">
                  <option v-for="o in customerOrderOptions" :key="o.id" :value="o.customerOrderNo"></option>
                </datalist>
                <small class="hint" v-if="selectedOrderProject">输入新客户订单号时，保存后系统会自动创建并绑定内部ID</small>
                <small class="hint" v-else>请先选择项目</small>
              </div>
              <div class="form-item"><label>客户电话</label><input v-model="orderForm.customerPhone" disabled /></div>
              <div class="form-item"><label>客户地址</label><input v-model="orderForm.customerAddress" disabled /></div>
            </div>

            <div class="inline-project-panel" v-if="orderInlineProjectVisible">
              <div class="section-title">填写项目</div>
              <div class="contract-form-grid">
                <div class="form-item"><label>项目名称 <span class="required-star">*</span></label><input v-model.trim="orderInlineProjectForm.projectName" placeholder="请输入项目名称；无具体项目可直接选择下拉选项" /></div>
                <div class="form-item"><label>项目地址</label><input v-model.trim="orderInlineProjectForm.projectAddress" placeholder="可选" /></div>
                <div class="form-item"><label>项目负责人</label><input v-model.trim="orderInlineProjectForm.projectManager" placeholder="可选" /></div>
                <div class="form-item"><label>备注</label><input v-model.trim="orderInlineProjectForm.remark" placeholder="可选" /></div>
              </div>
              <div class="actions end">
                <button class="ghost" @click="hideInlineProjectCreate">取消填写</button>
                <button class="primary" @click="saveInlineProject">保存并选中项目</button>
              </div>
            </div>

            <div class="section-title">订单识别信息</div>
            <div class="contract-form-grid">
              <div class="form-item">
                <label>工厂订单号</label>
                <div class="inline-field">
                  <div class="prefix-switch">
                    <button :class="{ active: orderForm.factoryOrderPrefix === 'L' }" @click="orderForm.factoryOrderPrefix = 'L'">L</button>
                    <button :class="{ active: orderForm.factoryOrderPrefix === 'V' }" @click="orderForm.factoryOrderPrefix = 'V'">V</button>
                  </div>
                  <input :value="`${orderForm.factoryOrderPrefix || 'V'} + 日期 + 流水号`" disabled />
                </div>
              </div>
              <div class="form-item span-2"><label>客户订单号</label><input v-model.trim="orderForm.customerOrderNo" placeholder="输入客户订单号后自动绑定" /></div>
            </div>

            <div class="section-title">业务信息</div>
            <div class="contract-form-grid">
              <div class="form-item">
                <label>负责人</label>
                <select v-model="orderForm.owner">
                  <option value="">请选择负责人</option>
                  <option v-for="s in ownerStaffOptions" :key="s.id || s.staff_code || s.staff_name" :value="s.staff_name">{{ s.staff_name }} / {{ s.staff_code || '-' }}</option>
                </select>
              </div>
              <div class="form-item"><label>接单日期</label><input v-model="orderForm.receiveDate" type="date" disabled /></div>
              <div class="form-item span-2"><label>需求说明</label><textarea v-model="orderForm.demandDesc" disabled></textarea></div>
            </div>

            <div class="section-title">报价分配</div>
            <div class="contract-form-grid">
              <div class="form-item"><label>客服</label><input v-model="assignmentForm.serviceStaff" /></div>
              <div class="form-item">
                <label>分配方式</label>
                <select v-model="orderAssignmentMode">
                  <option value="ASSIGNED">指定深化工程师</option>
                  <option value="POOL">先不指定，放入订单池</option>
                </select>
              </div>
              <div class="form-item" v-if="orderAssignmentMode === 'ASSIGNED'">
                <label>深化工程师</label>
                <select v-model="selectedEngineer">
                  <option value="">请选择工程师</option>
                  <option v-for="s in engineerOptions" :key="s.id" :value="s.staff_name">{{ s.staff_name }} / {{ s.staff_code }}</option>
                </select>
              </div>
              <div class="form-item" v-else>
                <label>深化工程师</label>
                <input value="进入深化订单池后由工程师领取" disabled />
              </div>
              <div class="form-item"><label>分配备注</label><input v-model="assignmentForm.remark" /></div>
              <div class="form-item span-2" v-if="orderAssignmentMode === 'POOL'"><label>&nbsp;</label><div class="hint">保存后该工厂订单进入“深化订单池”，深化工程师可在 4.3 领取。</div></div>
            </div>

            <div class="contract-bottom-bar">
              <span>已填写 {{ orderFormFilledCount }}/{{ orderFormTotalCount }} 项</span>
              <div>
                <button @click="switchToNewMasterContract">取消</button>
                <button @click="createOrder">保存</button>
                <button v-if="orderAssignmentMode === 'ASSIGNED'" class="primary" @click="createOrder(true)">保存并进入报价</button>
              </div>
            </div>
          </div>
        </div>

        <table>
          <thead>
            <tr>
              <th>项目名称</th><th>客户订单号</th><th>工厂订单号</th><th>客户项目号</th><th>客户名称</th><th>负责人</th><th>当前状态</th><th>创建时间</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredOrderRows" :key="row.id">
              <td><button class="link-btn" @click="openProjectDetail(row)">{{ row.projectName || row.projectNameSnapshot || '-' }}</button></td>
              <td>{{ getCustomerOrderNo(row) }}</td>
              <td>{{ getFactoryOrderNo(row) }}</td>
              <td><button class="link-btn" @click="openProjectDetail(row)">{{ row.projectNo || '-' }}</button></td>
              <td>{{ row.customerName }}</td>
              <td>{{ getOrderOwner(row) }}</td>
              <td><span class="status-pill" :class="orderStatusClass(row.status)">{{ row.status }}</span></td>
              <td>{{ formatDateTime(row.createdAt) }}</td>
              <td>
                <button class="mini" @click="openOrderDetailEdit(row)">详情</button>
                <button class="mini" @click="openOrderEdit(row)">编辑</button>
                <button class="mini" @click="goQuoteForOrder(row)">报价</button>
                <button class="mini danger" @click="requestDeleteOrder(row)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.order && activeSubModule === '订单状态'">
        <div class="table-tools">
          <button @click="loadOrders">刷新</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>订单号</th><th>负责人</th><th>当前状态</th><th>进度</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredOrderRows" :key="row.id">
              <td>{{ getFactoryOrderNo(row) }}</td>
              <td>{{ getOrderOwner(row) }}</td>
              <td><span class="status-pill" :class="orderStatusClass(row.status)">{{ row.status }}</span></td>
              <td>
                <span class="assignment-progress" :title="orderProgressTooltip(row)">{{ orderProgressText(row) }}</span>
              </td>
              <td>
                <select v-model="row._nextStatus">
                  <option value="">选择下一状态</option>
                  <option v-for="s in nextStatuses(row.status)" :key="s" :value="s">{{ s }}</option>
                </select>
                <input v-if="row._nextStatus==='生产中'" v-model.number="row._progress" type="number" min="0" max="100" placeholder="进度%" style="width:90px" />
                <button class="mini" @click="advanceOrderStatus(row)">更新状态</button>
                <button class="mini" @click="openOrderLogs(row)">日志</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.quote && activeSubModule === '报价单主表'">
        <h3 style="margin:0 0 10px;color:#1e3a8a;">订单汇总表（同一订单可多人报价）</h3>
        <div class="table-tools labeled-tools" style="margin-bottom:8px;">
          <div class="field-box compact">
            <label>选择项目 / 客户订单号 / 工厂订单号</label>
            <select v-model="quoteMainContractNo">
              <option value="">全部订单</option>
              <option v-for="r in quoteContractSummaryRows" :key="`main_${r.contract_no}`" :value="r.contract_no">
                {{ r.project_name || '-' }} / {{ r.customer_order_no || '-' }} / {{ r.contract_no }}
              </option>
            </select>
          </div>
          <button @click="refreshQuoteSummary">刷新</button>
        </div>

        <div class="quote-main-order-info" v-if="quoteMainInfo">
          <div><span>项目名称</span><strong>{{ quoteMainInfo.project_name || '-' }}</strong></div>
          <div><span>客户订单号</span><strong>{{ quoteMainInfo.customer_order_no || '-' }}</strong></div>
          <div><span>工厂订单号</span><strong>{{ quoteMainInfo.contract_no || '-' }}</strong></div>
          <div><span>客户名称</span><strong>{{ quoteMainInfo.customer_name || '-' }}</strong></div>
          <div><span>联系电话</span><strong>{{ quoteMainInfo.customer_phone || '-' }}</strong></div>
          <div><span>客户地址</span><strong>{{ quoteMainInfo.customer_address || '-' }}</strong></div>
          <div><span>分配工程师</span><strong>{{ quoteMainInfo.engineersText || '-' }}</strong></div>
          <div><span>完成进度</span><strong>{{ quoteMainInfo.progressText || '-/-' }}</strong></div>
          <div class="amount-emphasis"><span>汇总金额</span><strong>{{ money(quoteMainInfo.total_amount || 0) }}</strong></div>
        </div>

        <table style="margin-bottom:12px;">
          <thead>
            <tr>
              <th>项目名称</th><th>客户订单号</th><th>工厂订单号</th><th>客户名称</th><th>分配工程师</th><th>报价完成</th><th>报价份数</th><th>汇总金额</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredQuoteContractSummaryRows" :key="row.contract_no + row.engineersText">
              <td>{{ row.project_name || '-' }}</td>
              <td>{{ row.customer_order_no || '-' }}</td>
              <td>{{ row.contract_no }}</td>
              <td>{{ row.customer_name }}</td>
              <td>{{ row.engineersText }}</td>
              <td><span class="assignment-progress" :title="row.pendingTooltip || '已全部完成报价'">{{ row.progressText || '-/-' }}</span></td>
              <td>{{ row.quote_count }}</td>
              <td><span class="quote-amount-emphasis">{{ money(row.total_amount) }}</span></td>
              <td><button class="mini" @click="openContractSummaryDetail(row)">查看总明细</button></td>
            </tr>
          </tbody>
        </table>

        <div class="table-tools labeled-tools">
          <div class="field-box compact">
            <label>报价单检索</label>
            <input v-model="quoteOrderKeyword" placeholder="工厂订单号/客户订单号/客户/报价员" @input="loadQuoteOrders" />
          </div>
          <button @click="loadQuoteOrders">查询</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>报价单ID</th><th>项目名称</th><th>客户订单号</th><th>工厂订单号</th><th>客户名称</th><th>客服</th><th>深化工程师</th><th>联系电话</th><th>报价员</th><th>报价日期</th><th>总金额</th><th>报价说明</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredQuoteOrderRows" :key="row.quote_id">
              <td>{{ row.quote_id }}</td>
              <td>{{ row.project_name || '-' }}</td>
              <td>{{ row.customer_order_no || '-' }}</td>
              <td>{{ row.contract_no || '-' }}</td>
              <td>{{ row.customer_name || '-' }}</td>
              <td>{{ row.service_staff || '-' }}</td>
              <td>{{ row.engineer || '-' }}</td>
              <td>{{ row.customer_phone || '-' }}</td>
              <td>{{ row.created_by || '-' }}</td>
              <td>{{ formatDateTime(row.created_at) }}</td>
              <td class="quote-amount">{{ money(row.total_amount) }}</td>
              <td>{{ row.quote_desc || '-' }}</td>
              <td>
                <button class="mini" title="查看" @click="openQuotePreview(row)">查看</button>
                <button class="mini" title="编辑" @click="startEditQuote(row)">编辑</button>
                <button class="mini" @click="openQuoteLogs(row)">日志</button>
                <button class="mini" @click="exportQuoteFromRow(row)">导出Excel</button>
                <button class="mini danger" title="删除" @click="openQuoteDelete(row)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="filteredQuoteOrderRows.length === 0" class="quote-empty">
          <div class="quote-empty-title">还没有保存好的报价单</div>
          <div class="hint">在“报价明细”保存后，会自动出现在这里，可查看并导出 Excel。</div>
        </div>
      </section>

      <section class="card" v-if="activeModule === C.quote && activeSubModule === '财务确认'">
        <div class="table-tools labeled-tools">
          <div class="field-box compact">
            <label>订单检索</label>
            <input v-model="financeKeyword" placeholder="工厂订单号/客户订单号/客户名称" />
          </div>
          <button @click="loadOrders">刷新订单</button>
          <button @click="loadQuoteOrders">刷新报价</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>项目名称</th><th>客户订单号</th><th>工厂订单号</th><th>客户名称</th><th>当前状态</th><th>应收金额</th><th>进账金额</th><th>剩余待付</th><th>财务账户/渠道</th><th>财务人员</th><th>备注</th><th>进账凭据</th><th>二次确认</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="group in financeGroupedRows" :key="`group_${group.key}`">
              <tr class="finance-group-row">
                <td colspan="14">{{ group.label }}（{{ group.rows.length }}）</td>
              </tr>
              <tr v-for="row in group.rows" :key="`fin_${group.key}_${row.id}`">
              <td>{{ row.projectName || row.projectNameSnapshot || '-' }}</td>
              <td>{{ getCustomerOrderNo(row) }}</td>
              <td>{{ getFactoryOrderNo(row) }}</td>
              <td>{{ row.customerName }}</td>
              <td><span class="status-pill" :class="orderStatusClass(row.status)">{{ row.status }}</span></td>
              <td><span class="quote-amount-emphasis">{{ money(financeExpectedAmount(row)) }}</span></td>
              <td>
                <input :class="{ 'partial-input': financeGap(row) > 0.01 }" v-model.number="financeForm(row.id).amount" type="number" min="0" step="0.01" />
                <div class="hint" v-if="financeGap(row) > 0.01">未到账：{{ money(financeGap(row)) }}（已记录后继续追踪）</div>
              </td>
              <td><span :class="financeGap(row) > 0.01 ? 'finance-gap-emphasis' : ''">{{ money(financeGap(row)) }}</span></td>
              <td><input v-model="financeForm(row.id).payChannel" placeholder="如：工行对公/支付宝" /></td>
              <td><input v-model="financeForm(row.id).operator" placeholder="财务姓名" /></td>
              <td><input v-model="financeForm(row.id).remark" placeholder="财务备注（可选）" /></td>
              <td class="finance-voucher-cell">
                <div class="drop-upload voucher-drop" tabindex="0" @dragover.prevent @drop.prevent="onDropFinanceVoucher(row, $event)" @paste="onPasteFinanceVoucher(row, $event)">
                  <div class="upload-tip">支持本地上传/拖拽/粘贴（图片/PDF，≤20MB）</div>
                  <label class="file-btn">上传凭据<input type="file" accept=".pdf,image/*" @change="onFinanceVoucherFileChange(row, $event)" /></label>
                </div>
                <div class="voucher-list" v-if="financeForm(row.id).vouchers.length">
                  <div class="voucher-item" v-for="v in financeForm(row.id).vouchers" :key="v.id">
                    <span class="voucher-name" :title="v.file_name">{{ v.file_name }}</span>
                    <button class="mini" @click="openFinanceVoucherPreview(v)">查看</button>
                    <button class="mini danger" @click="removeFinanceVoucher(v)">删除</button>
                  </div>
                </div>
              </td>
              <td class="finance-confirm-cell">
                <label class="finance-confirm-label"><input type="checkbox" v-model="financeForm(row.id).confirmed" /> 已核对无误</label>
                <label class="finance-confirm-label partial-approve"><input type="checkbox" v-model="financeForm(row.id).allowPartialProduction" /> 不足额特批排产</label>
                <input class="finance-approver-input" v-model="financeForm(row.id).approver" placeholder="高权限审批人" />
                <input class="finance-approver-input" v-model="financeForm(row.id).approvalRemark" placeholder="审批原因（必填）" />
              </td>
              <td><button class="primary mini" @click="openFinanceConfirm(row)">审核完成</button></td>
            </tr>
            </template>
          </tbody>
        </table>
      </section>

      <section class="card" v-if="activeModule === C.quote && activeSubModule === '报价明细'">
        <div class="table-tools labeled-tools">
          <div class="field-box compact">
            <label>合同检索</label>
            <input v-model="contractKeyword" placeholder="工厂订单号/客户订单号/客户名" @input="searchContracts" />
          </div>
          <div class="field-box compact">
            <label>工厂订单号</label>
            <select v-model="quoteForm.contractId" @change="onContractChange($event)">
            <option value="">请选择工厂订单号</option>
            <option v-for="c in contractOptions" :key="c.id" :value="String(c.id)">{{ getFactoryOrderNo(c) }} / {{ getCustomerOrderNo(c) }} / {{ c.customerName }}</option>
          </select>
          </div>
          <div class="field-box compact">
            <label>深化工程师</label>
            <select v-model="quoteForm.assignmentId" @change="onAssignmentChange">
              <option value="">{{ quoteForm.contractId ? '请选择深化工程师' : '请先选择工厂订单号' }}</option>
              <option v-for="a in quoteAssignments" :key="a.id" :value="String(a.id)">
                {{ a.engineer || '-' }} / {{ quoteAssignmentStatusText(a.status || '已分配') }}
              </option>
            </select>
            <small v-if="quoteForm.contractId && !quoteAssignments.length" class="hint">该订单暂无报价分配，请先在 3.1 新建工厂订单时分配工程师</small>
          </div>
          <div class="field-box compact">
            <label>报价员</label>
            <input v-model="quoteForm.operator" placeholder="请输入报价员" />
          </div>
          <div class="field-box compact">
            <label>报价说明</label>
            <input v-model="quoteForm.quoteDesc" placeholder="可选" />
          </div>
          <button v-if="isAdvancedUser" @click="recalcCurrentContractQuotes">整合同重算</button>
          <button @click="saveQuoteDraft">{{ quoteEditingId ? '暂存修改' : '暂存报价' }}</button>
          <button class="primary" @click="openQuoteSubmitConfirm">{{ quoteEditingId ? '提交修改' : '提交报价单' }}</button>
        </div>

        <div v-if="!quoteForm.contractId" class="quote-empty">
          <div class="quote-empty-title">请选择工厂订单号后进入报价明细填报</div>
          <div class="hint">报价明细会绑定工厂订单号，订单信息、CAD 文件和报价行会一起归档。</div>
        </div>

        <template v-else>
        <div class="quote-sheet-head">
          <div class="sheet-cell"><span>项目名称</span><strong>{{ selectedContract?.projectName || selectedContract?.projectNameSnapshot || '-' }}</strong></div>
          <div class="sheet-cell"><span>客户订单号</span><strong>{{ getCustomerOrderNo(selectedContract) }}</strong></div>
          <div class="sheet-cell"><span>工厂订单号</span><strong>{{ getFactoryOrderNo(selectedContract) }}</strong></div>
          <div class="sheet-cell"><span>客户名称</span><strong>{{ selectedContract?.customerName || '-' }}</strong></div>
          <div class="sheet-cell"><span>客户电话</span><strong>{{ selectedContract?.customerPhone || '-' }}</strong></div>
          <div class="sheet-cell"><span>客户地址</span><strong>{{ selectedContract?.customerAddress || '-' }}</strong></div>
          <div class="sheet-cell"><span>需求说明</span><strong>{{ selectedContract?.demandDesc || '-' }}</strong></div>
          <div class="sheet-cell"><span>负责人</span><strong>{{ getOrderOwner(selectedContract) }}</strong></div>
          <div class="sheet-cell"><span>报价员</span><strong>{{ quoteForm.operator || '-' }}</strong></div>
          <div class="sheet-cell"><span>报价日期</span><strong>{{ quoteDateText }}</strong></div>
          <div class="sheet-cell"><span>产品大类</span><strong>{{ quoteForm.category || '-' }}</strong></div>
          <div class="sheet-cell"><span>合计金额</span><strong>{{ quoteTotalAmount.toFixed(2) }}</strong></div>
        </div>

        <div class="quote-category-tabs">
          <div v-for="v in quoteCategoryOptions" :key="v" class="category-tab-wrap">
            <button
              class="category-tab"
              :class="{ active: quoteForm.category === v }"
              @click="onQuoteCategoryTabClick(v)"
            >{{ v }}</button>
          </div>
        </div>
        <div class="quote-category-actions">
          <button :disabled="!quoteForm.contractId" @click="addQuoteItem">新增行</button>
          <button
            :disabled="!quoteForm.contractId || !quoteItems.length"
            :class="quoteBatchDeleteMode ? 'primary' : 'danger'"
            @click="onBatchDeleteClick"
          >{{ quoteBatchDeleteMode ? '确认批量删除' : '批量删除' }}</button>
          <button v-if="quoteBatchDeleteMode" class="ghost" @click="exitQuoteBatchDeleteMode">取消</button>
          <button :disabled="!quoteForm.contractId" @click="openAddCategoryDialog">新增大类</button>
        </div>

        <div class="cad-panel">
          <div class="cad-title">CAD 图纸（合同绑定）</div>
          <div class="drop-upload" tabindex="0" @paste="onPasteCad($event)" @dragover.prevent="cadDrag=true" @dragleave.prevent="cadDrag=false" @drop.prevent="onDropCad($event)" :class="{ dragover: cadDrag }">
            <div>拖拽 CAD/PDF/图片 到这里上传</div>
            <div class="upload-tip">也支持 Ctrl+V 粘贴图片；DWG/DXF 可上传归档，建议同时上传 PDF 便于预览</div>
            <label class="file-btn">选择文件上传<input type="file" accept=".dwg,.dxf,.pdf,image/*" @change="onCadFileSelect($event)" /></label>
          </div>
          <div class="cad-list">
            <div class="cad-item" v-for="f in cadFiles" :key="f.id">
              <span>{{ f.file_name }}</span>
              <button class="mini" @click="openCadPreview(f)">预览</button>
            </div>
          </div>
        </div>
        <div class="quote-sheet-scroll">
          <table class="quote-sheet-table">
            <thead>
              <tr>
                <th v-if="quoteBatchDeleteMode" class="quote-select-col">选择</th><th>序号</th><th>产品大类</th><th>产品风格</th><th>产品名称*</th><th>材质</th><th>颜色</th><th>拉手颜色</th><th>宽</th><th>高</th><th>厚</th><th>数量</th><th>平数</th><th>备注</th><th>非标</th><th>基础单价</th><th>金额</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="(row, idx) in quoteItems" :key="row.uid">
                <tr>
                  <td v-if="quoteBatchDeleteMode" class="quote-select-col"><input type="checkbox" :checked="selectedQuoteRowIds.includes(row.uid)" @change="toggleSelectQuoteRow(row.uid, $event)" /></td>
                  <td>{{ idx + 1 }}</td>
                  <td><input v-model="row.category" list="quoteCategoryList" @change="syncRowCategory(row)" /></td>
                  <td><input v-model="row.productType" :list="rowOptionListId(row, 'style')" placeholder="请选择风格" @change="syncRowProductStyle(row)" /></td>
                  <td><input v-model="row.modelKeyword" :list="rowOptionListId(row, 'model')" @change="applyByModel(row)" /></td>
                  <td><input v-model="row.materialStructure" :list="rowOptionListId(row, 'material')" /></td>
                  <td><input v-model="row.color" :list="rowOptionListId(row, 'color')" /></td>
                  <td><input v-model="row.handleColor" :list="rowOptionListId(row, 'handleColor')" /></td>
                  <td><input v-model.number="row.widthMm" type="number" min="0" /></td>
                  <td><input v-model.number="row.heightMm" type="number" min="0" /></td>
                  <td><input v-model.number="row.thicknessMm" type="number" min="0" /></td>
                  <td><input v-model.number="row.quantity" type="number" min="1" /></td>
                  <td>{{ Number(row.areaM2 || 0).toFixed(4) }}</td>
                  <td><input v-model="row.remark" /></td>
                  <td><select v-model="row.nonStandard" @change="onNonStandardChange(row)"><option :value="false">否</option><option :value="true">是</option></select></td>
                  <td><input v-model.number="row.baseUnitPrice" type="number" min="0" /></td>
                  <td>{{ Number(row.amount || 0).toFixed(2) }}</td>
                  <td><button class="mini" @click="calcQuoteRow(row)">计算</button><button class="mini danger" @click="requestRemoveQuoteItem(row.uid)">删除</button></td>
                </tr>
                <tr class="formula-row">
                  <td :colspan="quoteBatchDeleteMode ? 18 : 17"><strong>总公式：</strong>{{ buildFormulaText(row) }}</td>
                </tr>
                <tr v-if="row.nonStandard" class="nonstandard-row">
                  <td :colspan="quoteBatchDeleteMode ? 18 : 17">
                    <QuoteExtraPricePanel
                      :row="row"
                      :selected-rules="selectedRulesForRow(row)"
                      :estimate-charge="(rule) => estimateRuleCharge(row, rule)"
                      :rule-formula="(rule) => ruleFormulaFragment(row, rule)"
                      @search-rules="searchRules(row)"
                      @add-rule="(rule) => addRuleToRow(row, rule)"
                      @remove-rule="(id) => removeRuleFromRow(row, id)"
                      @remove-custom="(index) => removeCustomRuleFromRow(row, index)"
                      @request-remove-item="(item) => requestRemoveExtraItem(row, item)"
                      @rule-change="onRuleItemChanged(row)"
                      @add-custom="addCustomRuleToRow(row)"
                      @save-custom="saveCustomRule(row)"
                    />
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        <datalist id="quoteCategoryList">
          <option v-for="v in quoteCategoryOptions" :key="v" :value="v"></option>
        </datalist>
        <template v-for="row in quoteItems" :key="`lists_${row.uid}`">
          <datalist :id="rowOptionListId(row, 'style')">
            <option v-for="v in rowProductStyleOptions(row)" :key="v" :value="v"></option>
          </datalist>
          <datalist :id="rowOptionListId(row, 'model')">
            <option v-for="v in rowModelOptions(row)" :key="v" :value="v"></option>
          </datalist>
          <datalist :id="rowOptionListId(row, 'material')">
            <option v-for="v in rowMaterialOptions(row)" :key="v" :value="v"></option>
          </datalist>
          <datalist :id="rowOptionListId(row, 'color')">
            <option v-for="v in rowColorOptions(row)" :key="v" :value="v"></option>
          </datalist>
          <datalist :id="rowOptionListId(row, 'handleColor')">
            <option v-for="v in rowHandleColorOptions(row)" :key="v" :value="v"></option>
          </datalist>
        </template>
        <div class="actions end" style="margin-top:8px">
          <span>合计金额：{{ quoteTotalAmount.toFixed(2) }}</span>
        </div>
        </template>
      </section>

      <section class="card" v-if="activeModule === C.quote && activeSubModule === '深化订单池'">
        <div class="table-tools labeled-tools">
          <div class="field-box compact">
            <label>订单检索</label>
            <input v-model.trim="quotePoolKeyword" placeholder="项目/客户订单号/工厂订单号/客户/客服" />
          </div>
          <button @click="loadQuotePool">刷新</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>项目名称</th><th>客户订单号</th><th>工厂订单号</th><th>客户名称</th><th>客服</th><th>需求说明</th><th>入池时间</th><th>备注</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in quotePoolFilteredRows" :key="row.id">
              <td>{{ row.order?.projectName || row.order?.projectNameSnapshot || row.project_name || '-' }}</td>
              <td>{{ getCustomerOrderNo(row.order) }}</td>
              <td>{{ getFactoryOrderNo(row.order) || row.contract_no || '-' }}</td>
              <td>{{ row.order?.customerName || '-' }}</td>
              <td>{{ row.service_staff || '-' }}</td>
              <td>{{ row.order?.demandDesc || '-' }}</td>
              <td>{{ formatDateTime(row.created_at) }}</td>
              <td>{{ row.remark || '-' }}</td>
              <td><button class="mini primary" @click="claimQuotePoolOrder(row)">领取并进入报价</button></td>
            </tr>
            <tr v-if="!quotePoolFilteredRows.length">
              <td colspan="9" class="hint">{{ quotePoolLoading ? '正在加载订单池...' : '暂无待领取工厂订单' }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="customer-profile-host" v-if="isCustomerArchivePage">
        <CustomerProjectProfile />
      </section>

      <section class="customer-profile-host" v-if="v3SubModules.has(activeSubModule)">
        <OrderFlowV3 :view="activeSubModule" />
      </section>

      <section class="card" v-if="activeModule !== C.master && !v3SubModules.has(activeSubModule) && !isCustomerArchivePage">
        <p>当前为「{{ activeSubModule }}」独立页面占位，后续按你的业务继续逐页补齐。</p>
      </section>
    </main>

    <div class="import-mask" v-if="importVisible" @click.self="importVisible = false">
      <div class="import-dialog">
        <div class="import-head">
          <h3>批量导入</h3>
          <button class="close-btn" @click="importVisible = false">×</button>
        </div>

        <div class="import-toolbar">
          <select v-model="importModule">
            <option :value="C.staff">人员管理</option>
            <option :value="C.product">产品资料</option>
            <option :value="C.materialData">材料资料</option>
          </select>
          <button class="ghost" @click="fillImportExample">填充示例</button>
          <label class="file-btn">选择CSV文件<input type="file" accept=".csv,text/csv" @change="onCsvImport($event.target.files?.[0])" /></label>
          <button class="primary" @click="onBatchImport">执行导入</button>
        </div>

        <textarea v-model="importText" class="import-box"></textarea>
        <p class="hint">支持两种方式：1）选择CSV后自动转换为JSON；2）直接粘贴JSON数组。</p>

        <div class="template-box">
          <div class="template-title">CSV示例（首行必须是字段名）</div>
          <pre>{{ getCsvTemplate(importModule) }}</pre>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="filterVisible" @click.self="filterVisible = false">
      <div class="dialog-sm">
        <div class="import-head"><h3>筛选条件</h3><button class="close-btn" @click="filterVisible = false">×</button></div>
        <div class="form-grid">
          <input v-model="filterForm.keyword" placeholder="关键词（名称/编码/型号/颜色）" />
          <input v-model="filterForm.status" placeholder="状态（如：启用/在职）" />
          <input v-model="filterForm.color" placeholder="颜色（产品/材料可用）" />
        </div>
        <div class="actions end"><button class="ghost" @click="resetFilter">重置</button><button class="primary" @click="applyFilter">应用筛选</button></div>
      </div>
    </div>

    <div class="import-mask" v-if="inlineDeleteConfirmVisible" @click.self="closeInlineDeleteConfirm">
      <div class="dialog-sm">
        <div class="import-head"><h3>删除确认</h3><button class="close-btn" @click="closeInlineDeleteConfirm">×</button></div>
        <p style="margin: 10px 0 14px;">{{ inlineDeleteConfirmText }}</p>
        <div class="actions end">
          <button class="ghost" @click="closeInlineDeleteConfirm">取消</button>
          <button class="danger" @click="confirmInlineDelete">确认删除</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="orderEditVisible" @click.self="orderEditVisible = false">
      <div class="dialog-sm">
        <div class="import-head"><h3>编辑工厂订单</h3><button class="close-btn" @click="orderEditVisible = false">×</button></div>
        <div class="form-grid labeled-grid">
          <div class="field-box"><label>工厂订单号</label><input v-model="orderEditForm.factoryOrderNo" /></div>
          <div class="field-box"><label>客户订单号</label><input v-model="orderEditForm.customerOrderNo" /></div>
          <div class="field-box"><label>客户名称</label><input v-model="orderEditForm.customerName" /></div>
          <div class="field-box"><label>客户电话</label><input v-model="orderEditForm.customerPhone" /></div>
          <div class="field-box"><label>客户地址</label><input v-model="orderEditForm.customerAddress" /></div>
          <div class="field-box"><label>需求说明</label><input v-model="orderEditForm.demandDesc" /></div>
          <div class="field-box"><label>客服</label><input v-model="orderEditForm.assignmentServiceStaff" /></div>
          <div class="field-box">
            <label>深化工程师</label>
            <select v-model="orderEditForm.assignmentEngineer">
              <option value="">请选择深化工程师</option>
              <option v-for="s in engineerOptions" :key="s.id || s.staff_code || s.staff_name" :value="s.staff_name">{{ s.staff_name }} / {{ s.staff_code || '-' }}</option>
            </select>
          </div>
          <div class="field-box span-2"><label>分配备注</label><input v-model="orderEditForm.assignmentRemark" /></div>
        </div>
        <div class="actions end">
          <button class="ghost" @click="orderEditVisible = false">取消</button>
          <button class="primary" @click="saveOrderEdit">保存</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="projectDialogVisible" @click.self="projectDialogVisible = false">
      <div class="dialog-sm">
        <div class="import-head"><h3>新建项目</h3><button class="close-btn" @click="projectDialogVisible = false">×</button></div>
        <div class="form-grid labeled-grid">
          <div class="field-box">
            <label>客户名称</label>
            <div class="customer-picker">
              <input v-model.trim="projectCustomerKeyword" :disabled="projectDialogSource === 'order'" placeholder="搜索客户名称/客户编码/手机号" @focus="openCustomerDropdown('project')" @input="onProjectCustomerKeywordInput" @blur="deferCloseCustomerDropdown('project')" />
              <div v-if="projectCustomerDropdownOpen && projectCustomerOptions.length && projectDialogSource !== 'order'" class="customer-dropdown">
                <button v-for="c in projectCustomerOptions" :key="c.id" type="button" @mousedown.prevent="selectProjectCustomer(c)">
                  <strong>{{ customerOptionText(c) }}</strong>
                </button>
              </div>
            </div>
          </div>
          <div class="field-box"><label>项目名称</label><input v-model.trim="projectForm.projectName" placeholder="留空自动生成" /></div>
          <div class="field-box"><label>客户项目号</label><input value="保存后系统自动生成" disabled /></div>
          <div class="field-box"><label>项目地址</label><input v-model="projectForm.projectAddress" /></div>
          <div class="field-box"><label>项目负责人</label><input v-model="projectForm.projectManager" /></div>
          <div class="field-box span-2"><label>备注</label><input v-model="projectForm.remark" /></div>
        </div>
        <div class="actions end">
          <button class="ghost" @click="projectDialogVisible = false">取消</button>
          <button class="primary" @click="saveProject">保存项目</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="projectDetailVisible" @click.self="projectDetailVisible = false">
      <div class="quote-preview-dialog">
        <div class="import-head"><h3>项目详情 - {{ projectDetail.projectNo || '-' }}</h3><button class="close-btn" @click="projectDetailVisible = false">×</button></div>
        <div class="project-detail-grid">
          <div><span>客户项目号</span><strong>{{ projectDetail.projectNo || '-' }}</strong></div>
          <div><span>项目名称</span><strong>{{ projectDetail.projectName || '-' }}</strong></div>
          <div><span>客户名称</span><strong>{{ projectDetail.customerName || '-' }}</strong></div>
          <div><span>项目状态</span><strong>{{ projectDetail.projectStatus || '进行中' }}</strong></div>
          <div><span>项目负责人</span><strong>{{ projectDetail.projectManager || '-' }}</strong></div>
          <div><span>订单数量</span><strong>{{ projectDetailOrders.length }}</strong></div>
          <div><span>创建时间</span><strong>{{ formatDateTime(projectDetail.createdAt) }}</strong></div>
        </div>
        <div class="project-tabs">
          <button :class="{ active: projectDetailTab === 'overview' }" @click="projectDetailTab = 'overview'">项目概览</button>
          <button :class="{ active: projectDetailTab === 'orders' }" @click="projectDetailTab = 'orders'">订单列表</button>
          <button :class="{ active: projectDetailTab === 'quote' }" @click="projectDetailTab = 'quote'">报价汇总</button>
          <button :class="{ active: projectDetailTab === 'payment' }" @click="projectDetailTab = 'payment'">收款汇总</button>
          <button :class="{ active: projectDetailTab === 'production' }" @click="projectDetailTab = 'production'">生产进度</button>
          <button :class="{ active: projectDetailTab === 'cad' }" @click="projectDetailTab = 'cad'">CAD 文件</button>
          <button :class="{ active: projectDetailTab === 'log' }" @click="projectDetailTab = 'log'">操作日志</button>
        </div>
        <div v-if="projectDetailTab === 'orders'">
          <table>
            <thead><tr><th>项目名称</th><th>客户订单号</th><th>工厂订单号</th><th>当前状态</th><th>负责人</th><th>报价状态</th><th>生产状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="row in projectDetailOrders" :key="row.id">
                <td>{{ row.projectName || row.projectNameSnapshot || '-' }}</td>
                <td>{{ getCustomerOrderNo(row) }}</td>
                <td>{{ getFactoryOrderNo(row) }}</td>
                <td><span class="status-pill" :class="orderStatusClass(row.status)">{{ row.status }}</span></td>
                <td>{{ getOrderOwner(row) }}</td>
                <td>{{ orderQuoteProgressText(row) }}</td>
                <td>{{ showProgressText(row) }}</td>
                <td><button class="mini" @click="goQuoteForOrder(row)">报价</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="hint project-placeholder">该模块入口已预留，将在后续汇总和日志联动中补齐。</div>
      </div>
    </div>

    <div class="import-mask" v-if="orderDetailEditVisible" @click.self="orderDetailEditVisible = false">
      <div class="quote-preview-dialog">
        <div class="import-head">
          <h3>子合同详情编辑（后续补填）</h3>
          <button class="close-btn" @click="orderDetailEditVisible = false">×</button>
        </div>
        <div class="form-grid labeled-grid">
          <div class="field-box"><label>总合同号</label><input v-model="orderDetailEditForm.masterContractNo" /></div>
          <div class="field-box"><label>客户姓名</label><input v-model="orderDetailEditForm.customerName" /></div>
          <div class="field-box"><label>客户电话</label><input v-model="orderDetailEditForm.customerPhone" /></div>
          <div class="field-box"><label>客户地址</label><input v-model="orderDetailEditForm.customerAddress" /></div>
          <div class="field-box"><label>客户订单号</label><input v-model="orderDetailEditForm.customerOrderNo" /></div>
          <div class="field-box"><label>负责人</label><input v-model="orderDetailEditForm.owner" /></div>
          <div class="field-box"><label>需求说明</label><input v-model="orderDetailEditForm.demandDesc" /></div>
          <div class="field-box"><label>门型</label><input v-model="orderDetailEditForm.doorType" /></div>
          <div class="field-box"><label>颜色</label><input v-model="orderDetailEditForm.colorName" /></div>
          <div class="field-box"><label>材质</label><input v-model="orderDetailEditForm.materialName" /></div>
          <div class="field-box"><label>色系</label><input v-model="orderDetailEditForm.colorSeries" /></div>
          <div class="field-box"><label>门板厚度</label><input v-model="orderDetailEditForm.doorThickness" /></div>
          <div class="field-box"><label>门型备注</label><input v-model="orderDetailEditForm.doorTypeRemark" /></div>
          <div class="field-box"><label>颜色备注</label><input v-model="orderDetailEditForm.colorRemark" /></div>
          <div class="field-box"><label>芯板类型</label><input v-model="orderDetailEditForm.coreBoardType" /></div>
          <div class="field-box"><label>木皮</label><input v-model="orderDetailEditForm.woodVeneer" /></div>
          <div class="field-box"><label>板材材质</label><input v-model="orderDetailEditForm.boardMaterial" /></div>
          <div class="field-box"><label>墙板型号</label><input v-model="orderDetailEditForm.wallPanelModel" /></div>
          <div class="field-box"><label>墙板材质</label><input v-model="orderDetailEditForm.wallPanelMaterial" /></div>
          <div class="field-box"><label>报价日期</label><input v-model="orderDetailEditForm.quoteDate" type="date" /></div>
          <div class="field-box"><label>接单日期</label><input v-model="orderDetailEditForm.receiveDate" type="date" /></div>
          <div class="field-box"><label>出厂日期</label><input v-model="orderDetailEditForm.exportDate" type="date" /></div>
          <div class="field-box"><label>报价员</label><input v-model="orderDetailEditForm.quoterName" /></div>
          <div class="field-box"><label>工期</label><input v-model="orderDetailEditForm.workDays" /></div>
          <div class="field-box"><label>门板工艺员</label><input v-model="orderDetailEditForm.doorTechnician" /></div>
          <div class="field-box"><label>小件工艺员</label><input v-model="orderDetailEditForm.smallPartTechnician" /></div>
        </div>
        <div class="actions end" style="margin-top:10px;">
          <button class="ghost" @click="orderDetailEditVisible = false">取消</button>
          <button class="primary" @click="saveOrderDetailEdit">保存详情</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="addCategoryVisible" @click.self="addCategoryVisible = false">
      <div class="dialog-sm">
        <div class="import-head"><h3>新增产品大类</h3><button class="close-btn" @click="addCategoryVisible = false">×</button></div>
        <div class="form-grid">
          <input v-model.trim="newCategoryName" placeholder="请输入大类名称（如：房门）" />
        </div>
        <div class="actions end">
          <button class="ghost" @click="addCategoryVisible = false">取消</button>
          <button class="primary" @click="submitAddCategory">确认新增</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="addVisible">
      <div class="dialog-sm">
        <div class="import-head"><h3>新增{{ addModule }}</h3><button class="close-btn" @click="addVisible = false">×</button></div>

        <div v-if="addModule === C.staff" class="form-grid">
          <input v-model="addStaff.staff_code" placeholder="人员编码" />
          <input v-model="addStaff.staff_name" placeholder="姓名" />
          <select v-model="addStaff.role_type">
            <option value="">选择角色</option>
            <option v-for="r in roleOptions" :key="r" :value="r">{{ r }}</option>
          </select>
          <input v-model="addStaff.process_name" placeholder="工序（车间可填）" />
          <input v-model="addStaff.phone" placeholder="电话" />
          <select v-model="addStaff.status"><option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option></select>
        </div>

        <div v-else-if="addModule === C.userAuth" class="form-grid">
          <input v-model.trim="addUser.username" placeholder="登录账号" />
          <input v-model.trim="addUser.displayName" placeholder="显示姓名" />
          <select v-model="addUser.roleCode">
            <option value="">选择角色</option>
            <option v-for="r in authRoleOptions" :key="r" :value="r">{{ r }}</option>
          </select>
          <input v-model="addUser.password" type="password" placeholder="初始密码（默认123456）" />
          <select v-model.number="addUser.enabled">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>

        <div v-else-if="addModule === C.product" class="form-grid">
          <input v-model="addProduct.product_code" placeholder="产品编码" />
          <input v-model="addProduct.type" placeholder="产品类型" />
          <input v-model="addProduct.product_name" placeholder="产品风格" />
          <input v-model="addProduct.model" placeholder="产品名称" />
          <input v-model="addProduct.material_name" placeholder="材质" />
          <input v-model="addProduct.color" placeholder="颜色" />
          <input v-model="addProduct.handle_color" placeholder="拉手颜色" />
          <input v-model="addProduct.unit_price" placeholder="单价" />
          <input v-model="addProduct.unit_price_unit" placeholder="单价单位（如：元/m²）" />
          <input v-model="addProduct.thickness" placeholder="厚度" />
          <input v-model="addProduct.thickness_unit" placeholder="厚度单位（如：mm）" />
          <input v-model="addProduct.size" placeholder="尺寸（如：1200mm*2700mm）" />
          <input v-model="addProduct.image_url" placeholder="图片地址（可上传自动生成）" />
          <div
            class="drop-upload"
            :class="{ dragover: dragState.product }"
            tabindex="0"
            @paste="onPasteAddImage('product', $event)"
            @dragover.prevent="dragState.product = true"
            @dragleave.prevent="dragState.product = false"
            @drop.prevent="onDropAddImage('product', $event)"
          >
            <div>拖动图片到这里上传</div>
            <div class="upload-tip">也支持 Ctrl+V 粘贴图片</div>
            <label class="file-btn">选择文件上传<input type="file" accept="image/*" @change="onAddFileChange('product', $event)" /></label>
          </div>
          <img v-if="(addPreview.product || addProduct.image_url)" :src="addPreview.product || toAbsUrl(addProduct.image_url)" class="thumb" />
          <button v-if="(addPreview.product || addProduct.image_url)" class="mini danger" @click="clearAddImage('product')">删除图片</button>
          <select v-model="addProduct.status"><option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option></select>
        </div>

        <div v-else-if="addModule === C.materialData" class="form-grid">
          <input v-model="addMaterial.material_code" placeholder="材料编码" />
          <input v-model="addMaterial.material_name" placeholder="材料名称" />
          <input v-model="addMaterial.material_type" placeholder="材质类型（芦花板/无醛板等）" />
          <input v-model="addMaterial.color" placeholder="颜色" />
          <input v-model="addMaterial.length_mm" placeholder="长(mm)" />
          <input v-model="addMaterial.width_mm" placeholder="宽(mm)" />
          <input v-model="addMaterial.thickness_mm" placeholder="厚(mm)" />
          <input v-model="addMaterial.image_url" placeholder="图片地址（可上传自动生成）" />
          <div
            class="drop-upload"
            :class="{ dragover: dragState.material }"
            tabindex="0"
            @paste="onPasteAddImage('material', $event)"
            @dragover.prevent="dragState.material = true"
            @dragleave.prevent="dragState.material = false"
            @drop.prevent="onDropAddImage('material', $event)"
          >
            <div>拖动图片到这里上传</div>
            <div class="upload-tip">也支持 Ctrl+V 粘贴图片</div>
            <label class="file-btn">选择文件上传<input type="file" accept="image/*" @change="onAddFileChange('material', $event)" /></label>
          </div>
          <img v-if="(addPreview.material || addMaterial.image_url)" :src="addPreview.material || toAbsUrl(addMaterial.image_url)" class="thumb" />
          <button v-if="(addPreview.material || addMaterial.image_url)" class="mini danger" @click="clearAddImage('material')">删除图片</button>
          <select v-model="addMaterial.status"><option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option></select>
        </div>

        <p v-else class="hint">当前二级页面暂未接入新增接口。</p>
        <div class="actions end">
          <button class="ghost" @click="addVisible = false">取消</button>
          <button class="primary" @click="submitAdd">保存</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="detailVisible">
      <div class="dialog-sm">
        <div class="import-head"><h3>详情</h3><button class="close-btn" @click="detailVisible = false">×</button></div>
        <div class="form-grid detail-grid">
          <div v-for="(val, key) in detailData" :key="key" v-show="!['id'].includes(key)">
            <label>{{ fieldLabel(key) }}</label>
            <div v-if="key === 'status'"><span class="status-pill" :class="statusClass(val)">{{ val || '暂存' }}</span></div>
            <div v-else-if="key === 'image_url'" class="detail-image-wrap">
              <img v-if="val" :src="toAbsUrl(val)" class="thumb detail-thumb previewable" @click="openImagePreview(toAbsUrl(val))" @error="onImageError" />
              <div v-else class="thumb empty-thumb">暂无</div>
              <div class="img-path">{{ val || '-' }}</div>
              <div class="actions">
                <button v-if="val && detailModule" class="mini" @click="openImagePreview(toAbsUrl(val))">放大</button>
                <button v-if="val && detailModule" class="mini danger" @click="removeDetailImage">删除图片</button>
              </div>
            </div>
            <div v-else>{{ val }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="editVisible">
      <div class="dialog-sm">
        <div class="import-head"><h3>编辑</h3><button class="close-btn" @click="editVisible = false">×</button></div>
        <div class="form-grid detail-grid">
          <div v-for="(val, key) in editData" :key="key" v-show="!['id', 'created_at', 'updated_at'].includes(key)">
            <label>{{ fieldLabel(key) }}</label>
            <select v-if="key === 'status'" v-model="editData[key]">
              <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
            </select>
            <select v-else-if="key === 'role_code'" v-model="editData[key]">
              <option v-for="r in authRoleOptions" :key="r" :value="r">{{ r }}</option>
            </select>
            <select v-else-if="key === 'enabled'" v-model.number="editData[key]">
              <option :value="1">启用</option>
              <option :value="0">禁用</option>
            </select>
            <div v-else-if="key === 'image_url'" class="form-grid image-edit-grid drop-upload" tabindex="0" @paste="onPasteEditImage($event)" @dragover.prevent @drop.prevent="onDropEditImage($event)">
              <input v-model="editData[key]" />
              <div class="upload-tip">支持拖拽或 Ctrl+V 粘贴图片到此区域</div>
              <label class="file-btn">上传图片<input type="file" accept="image/*" @change="onEditImageUpload($event)" /></label>
              <img v-if="editData.image_url" :src="toAbsUrl(editData.image_url)" class="thumb detail-thumb previewable" @click="openImagePreview(toAbsUrl(editData.image_url))" @error="onImageError" />
              <div class="actions">
                <button v-if="editData.image_url" class="mini" @click="openImagePreview(toAbsUrl(editData.image_url))">放大</button>
                <button v-if="editData.image_url" class="mini danger" @click="editData.image_url = ''">删除图片</button>
              </div>
            </div>
            <input v-else v-model="editData[key]" />
          </div>
        </div>
        <div class="actions end">
          <button class="ghost" @click="editVisible = false">取消</button>
          <button class="primary" @click="submitEdit">保存</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="ruleEditVisible" @click.self="ruleEditVisible = false">
      <div class="dialog-sm">
        <div class="import-head"><h3>编辑价格规则</h3><button class="close-btn" @click="ruleEditVisible = false">×</button></div>
        <div class="form-grid labeled-grid">
          <div class="field-box"><label>规则大类</label><input v-model="ruleEditForm.ruleCategory" list="quoteCategoryList" /></div>
          <div class="field-box"><label>规则名称</label><input v-model="ruleEditForm.ruleName" /></div>
          <div class="field-box"><label>计价模式</label><select v-model="ruleEditForm.adjustMode">
            <option value="FIXED_PER_M2">按平米</option>
            <option value="PERCENT">按百分比</option>
            <option value="FIXED_PER_ITEM">按件</option>
          </select></div>
          <div class="field-box"><label>增减值</label><input v-model.number="ruleEditForm.adjustValue" type="number" /></div>
          <div class="field-box"><label>单位说明</label><input v-model="ruleEditForm.unitDesc" /></div>
          <div class="field-box"><label>最小计费面积</label><input v-model.number="ruleEditForm.minAreaM2" type="number" step="0.0001" /></div>
          <div class="field-box"><label>最低费用</label><input v-model.number="ruleEditForm.minCharge" type="number" /></div>
          <div class="field-box"><label>最高费用</label><input v-model.number="ruleEditForm.maxCharge" type="number" /></div>
          <div class="field-box"><label>状态</label><select v-model="ruleEditForm.status"><option v-for="s in ruleStatusOptions" :key="s" :value="s">{{ s }}</option></select></div>
          <div class="field-box"><label>备注</label><input v-model="ruleEditForm.remark" /></div>
        </div>
        <div class="actions end">
          <button class="ghost" @click="ruleEditVisible = false">取消</button>
          <button class="primary" @click="submitRuleEdit">保存</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="ruleDetailVisible" @click.self="ruleDetailVisible = false">
      <div class="dialog-sm">
        <div class="import-head"><h3>价格规则详情</h3><button class="close-btn" @click="ruleDetailVisible = false">×</button></div>
        <div class="form-grid detail-grid">
          <div><label>规则编码</label><div>{{ ruleDetailData.rule_code }}</div></div>
          <div><label>规则大类</label><div>{{ ruleDetailData.rule_category || '-' }}</div></div>
          <div><label>规则名称</label><div>{{ ruleDetailData.rule_name }}</div></div>
          <div><label>计价模式</label><div>{{ ruleModeText(ruleDetailData.adjust_mode) }}</div></div>
          <div><label>增减值</label><div>{{ ruleDetailData.adjust_value }}</div></div>
          <div><label>单位说明</label><div>{{ ruleDetailData.unit_desc || '-' }}</div></div>
          <div><label>最小面积</label><div>{{ ruleDetailData.min_area_m2 ?? '-' }}</div></div>
          <div><label>最低费用</label><div>{{ ruleDetailData.min_charge ?? '-' }}</div></div>
          <div><label>最高费用</label><div>{{ ruleDetailData.max_charge ?? '-' }}</div></div>
          <div><label>状态</label><div><span class="status-pill" :class="ruleStatusClass(ruleDetailData.enabled)">{{ ruleStatusText(ruleDetailData.enabled) }}</span></div></div>
          <div><label>备注</label><div>{{ ruleDetailData.remark || '-' }}</div></div>
          <div><label>更新时间</label><div>{{ formatDateTime(ruleDetailData.updated_at) }}</div></div>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="quotePreviewVisible" @click.self="quotePreviewVisible = false">
      <div class="quote-preview-dialog">
        <div class="import-head">
          <h3>报价单主表 - {{ quotePreviewMain.contract_no || quotePreviewMain.quote_id }}</h3>
          <div class="actions">
            <button class="primary" @click="exportQuoteExcel">导出Excel</button>
            <button class="close-btn" @click="quotePreviewVisible = false">×</button>
          </div>
        </div>
        <div class="quote-preview-sheet" id="quotePreviewSheet">
          <div class="quote-sheet-logo-wrap">
            <img :src="brandLogo" alt="龙泽伟尼" class="quote-sheet-logo" />
          </div>
          <table class="quote-preview-meta">
            <tbody>
              <tr>
                <th>工厂订单号</th><td>{{ quotePreviewMain.contract_no || '-' }}</td>
                <th>客户订单号</th><td>{{ quotePreviewMain.customer_order_no || '-' }}</td>
                <th>接单日期</th><td>{{ formatDateOnly(quotePreviewMain.created_at) }}</td>
                <th>报价日期</th><td>{{ formatDateOnly(quotePreviewMain.created_at) }}</td>
                <th>报价员</th><td>{{ quotePreviewMain.created_by || '-' }}</td>
              </tr>
              <tr>
                <th>客服</th><td>{{ quotePreviewMain.service_staff || '-' }}</td>
                <th>深化工程师</th><td>{{ quotePreviewMain.engineer || '-' }}</td>
                <th>分配ID</th><td>{{ quotePreviewMain.assignment_id || '-' }}</td>
                <th> </th><td></td>
                <th> </th><td></td>
              </tr>
              <tr>
                <th>门型编号</th><td>{{ firstQuoteDetail.attachment_name || '-' }}</td>
                <th>门型说明</th><td colspan="3">{{ quotePreviewMain.quote_desc || '-' }}</td>
                <th>材质</th><td>{{ firstQuoteDetail.material_structure || '-' }}</td>
                <th>客户名称</th><td>{{ quotePreviewMain.customer_name || '-' }}</td>
              </tr>
              <tr>
                <th>颜色编号</th><td colspan="3">{{ previewColorText }}</td>
                <th>颜色说明</th><td colspan="2">{{ previewColorText }}</td>
                <th>发货地址</th><td>{{ quotePreviewMain.customer_address || '-' }}</td>
                <th>关注类型</th><td></td>
              </tr>
              <tr>
                <th>特殊备注</th><td colspan="5">{{ quotePreviewMain.demand_desc || '-' }}</td>
                <th>生产周期</th><td>70天</td>
                <th>联系电话</th><td colspan="2">{{ quotePreviewMain.customer_phone || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <table class="quote-preview-lines">
            <thead>
              <tr>
                <th>序号</th><th>产品名称</th><th>材质结构</th><th>拉手颜色</th><th>宽</th><th>高</th><th>厚</th><th>数量</th><th>铰链孔</th><th>工艺说明</th><th>附图</th><th>单位</th><th>总量</th><th>单价</th><th>金额</th><th>备注</th><th>生产工序</th><th>技术员</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in quotePreviewDetails" :key="row.id || idx">
                <td>{{ idx + 1 }}</td>
                <td>{{ row.product_name }}</td>
                <td>{{ row.material_structure }}</td>
                <td>{{ row.handle_color || '-' }}</td>
                <td>{{ row.width_mm }}</td>
                <td>{{ row.height_mm }}</td>
                <td>{{ row.thickness_mm }}</td>
                <td>{{ row.quantity }}</td>
                <td>{{ row.hinge_hole || '-' }}</td>
                <td class="left-text">{{ row.process_desc || '-' }}</td>
                <td>{{ row.attachment_name || '-' }}</td>
                <td>{{ row.unit || 'm²' }}</td>
                <td>{{ row.area_m2 }}</td>
                <td>{{ row.final_unit_price }}</td>
                <td>{{ row.amount }}</td>
                <td class="left-text">{{ quoteFormulaForSaved(row) }}</td>
                <td></td>
                <td></td>
              </tr>
              <tr class="quote-total-row">
                <td colspan="7">合计</td>
                <td>{{ quotePreviewDetails.reduce((sum, row) => sum + Number(row.quantity || 0), 0) }}</td>
                <td colspan="4"></td>
                <td>{{ quotePreviewDetails.reduce((sum, row) => sum + Number(row.area_m2 || 0), 0).toFixed(4) }}</td>
                <td></td>
                <td>{{ money(quotePreviewMain.total_amount) }}</td>
                <td colspan="3"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="quoteDeleteVisible" @click.self="quoteDeleteVisible = false">
      <div class="dialog-sm">
        <div class="import-head">
          <h3>删除报价单二次确认</h3>
          <button class="close-btn" @click="quoteDeleteVisible = false">×</button>
        </div>
        <div class="delete-warning">
          <strong>即将删除报价单：{{ quoteDeleteTarget.contract_no || quoteDeleteTarget.quote_id }}</strong>
          <p>删除后会同时删除该报价单下的所有报价明细，此操作不可恢复。</p>
        </div>
        <div class="form-grid labeled-grid">
          <div class="field-box">
            <label>动态验证码</label>
            <div class="captcha-row">
              <span class="captcha-code">{{ quoteDeleteCaptcha }}</span>
              <button class="ghost" @click="refreshQuoteDeleteCaptcha">刷新</button>
            </div>
          </div>
          <div class="field-box">
            <label>请输入验证码</label>
            <input v-model="quoteDeleteInput" placeholder="输入左侧验证码后才能删除" />
          </div>
        </div>
        <div class="actions end">
          <button class="ghost" @click="quoteDeleteVisible = false">取消</button>
          <button class="danger" @click="confirmQuoteDelete">确认删除</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="financeConfirmVisible" @click.self="financeConfirmVisible = false">
      <div class="dialog-sm">
        <div class="import-head">
          <h3>财务审核二次确认</h3>
          <button class="close-btn" @click="financeConfirmVisible = false">×</button>
        </div>
        <div class="delete-warning">
          <strong>工厂订单号：{{ financeConfirmTarget?.factoryOrderNo || financeConfirmTarget?.contractNo || '-' }}</strong>
          <p>请确认本次审核信息无误，输入验证码后才能执行“审核完成”。</p>
        </div>
        <div class="form-grid labeled-grid">
          <div class="field-box">
            <label>动态验证码</label>
            <div class="captcha-row">
              <span class="captcha-code">{{ financeConfirmCaptcha }}</span>
              <button class="ghost" @click="refreshFinanceConfirmCaptcha">刷新</button>
            </div>
          </div>
          <div class="field-box">
            <label>请输入验证码</label>
            <input v-model="financeConfirmInput" placeholder="输入左侧验证码后才能提交审核" />
          </div>
        </div>
        <div class="actions end">
          <button class="ghost" @click="financeConfirmVisible = false">取消</button>
          <button class="primary" @click="confirmFinanceAudit">确认审核</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="quoteSubmitConfirmVisible" @click.self="quoteSubmitConfirmVisible = false">
      <div class="dialog-sm">
        <div class="import-head">
          <h3>深化报价提交二次确认</h3>
          <button class="close-btn" @click="quoteSubmitConfirmVisible = false">×</button>
        </div>
        <div class="delete-warning">
          <strong>请确认后提交报价</strong>
          <p>提交后会写入报价日志，并在满足条件时自动推进订单状态。</p>
        </div>
        <div class="form-grid">
          <div class="field-box">
            <label>动态验证码</label>
            <div class="captcha-row">
              <span class="captcha-code">{{ quoteSubmitCaptcha }}</span>
              <button class="ghost" @click="refreshQuoteSubmitCaptcha">刷新</button>
            </div>
          </div>
          <div class="field-box">
            <label>请输入验证码</label>
            <input v-model="quoteSubmitInput" placeholder="输入验证码后才能提交报价" />
          </div>
        </div>
        <div class="actions end">
          <button class="ghost" @click="quoteSubmitConfirmVisible = false">取消</button>
          <button class="primary" @click="confirmQuoteSubmit">确认提交</button>
        </div>
      </div>
    </div>

    <div class="import-mask" v-if="previewVisible" @click.self="closeImagePreview">
      <div class="preview-dialog">
        <img :src="previewSrc" class="preview-image" :style="{ transform: `scale(${previewScale})` }" @wheel.prevent="onPreviewWheel" />
      </div>
    </div>

    <div class="import-mask" v-if="cadPreviewVisible" @click.self="closeCadPreview">
      <div class="preview-dialog cad-preview-dialog">
        <div class="actions end" style="width:100%">
          <button class="ghost" @click="zoomCad(0.1)">放大</button>
          <button class="ghost" @click="zoomCad(-0.1)">缩小</button>
          <button class="ghost" @click="cadScale=1">重置</button>
        </div>
        <img v-if="cadPreviewType==='image'" :src="cadPreviewSrc" class="preview-image" :style="{ transform: `scale(${cadScale})` }" @wheel.prevent="onCadWheel" />
        <iframe v-else-if="cadPreviewType==='pdf'" :src="cadPreviewSrc" class="cad-pdf" :style="{ transform: `scale(${cadScale})` }"></iframe>
        <div v-else class="hint">该文件类型暂不支持浏览器预览，请下载到本地 CAD 软件打开。</div>
      </div>
    </div>

    <div class="import-mask" v-if="orderLogsVisible" @click.self="orderLogsVisible=false">
      <div class="dialog-sm">
        <div class="import-head"><h3>订单状态日志 - {{ currentOrderNo }}</h3><button class="close-btn" @click="orderLogsVisible=false">×</button></div>
        <table>
          <thead><tr><th>时间</th><th>类型</th><th>动作</th><th>从状态</th><th>到状态</th><th>操作人</th><th>备注</th></tr></thead>
          <tbody>
            <tr v-for="l in orderLogs" :key="l.id">
              <td>{{ formatDateTime(l.created_at || l.createdAt) }}</td>
              <td>{{ l.log_type === 'WORKFLOW' ? '状态流转' : '业务修改' }}</td>
              <td>{{ l.action || '-' }}</td>
              <td>{{ l.from_status || l.fromStatus || '-' }}</td>
              <td>{{ l.to_status || l.toStatus || '-' }}</td>
              <td>{{ l.operator || '-' }}</td>
              <td>{{ l.remark || '-' }}</td>
            </tr>
            <tr v-if="!orderLogs.length">
              <td colspan="7" class="hint">暂无日志记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="import-mask" v-if="quoteLogsVisible" @click.self="quoteLogsVisible=false">
      <div class="dialog-sm">
        <div class="import-head"><h3>报价单操作日志 - {{ quoteLogsTarget.contract_no || quoteLogsTarget.quote_id }}</h3><button class="close-btn" @click="quoteLogsVisible=false">×</button></div>
        <table>
          <thead><tr><th>时间</th><th>动作</th><th>操作人</th><th>备注</th></tr></thead>
          <tbody>
            <tr v-for="l in quoteLogsRows" :key="l.id">
              <td>{{ formatDateTime(l.created_at) }}</td>
              <td>{{ l.action }}</td>
              <td>{{ l.operator }}</td>
              <td>{{ l.remark || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="import-mask" v-if="summaryDetailVisible" @click.self="summaryDetailVisible=false">
      <div class="dialog-sm summary-dialog">
        <div class="import-head">
          <h3>报价单主表 - {{ summaryDetailContractNo }}</h3>
          <div class="actions">
            <button class="primary" @click="exportSummaryDetailExcel">导出汇总Excel</button>
            <button class="close-btn" @click="summaryDetailVisible=false">×</button>
          </div>
        </div>
        <div class="quote-preview-sheet">
          <table class="quote-preview-meta">
            <tbody>
              <tr>
                <th>工厂订单号</th><td>{{ summaryDetailInfo?.contract_no || '-' }}</td>
                <th>客户订单号</th><td>{{ summaryDetailInfo?.customer_order_no || '-' }}</td>
                <th>接单日期</th><td>{{ formatDateOnly(summaryDetailInfo?.created_at || summaryDetailRows[0]?.created_at) }}</td>
                <th>报价日期</th><td>{{ formatDateOnly(summaryDetailRows[0]?.created_at) }}</td>
                <th>报价份数</th><td>{{ summaryDetailRows.length }}</td>
              </tr>
              <tr>
                <th>客服</th><td>{{ summaryDetailRows[0]?.service_staff || '-' }}</td>
                <th>深化工程师</th><td>{{ summaryDetailInfo?.engineersText || '-' }}</td>
                <th>客户名称</th><td>{{ summaryDetailInfo?.customer_name || '-' }}</td>
                <th>联系电话</th><td>{{ summaryDetailRows[0]?.customer_phone || '-' }}</td>
              </tr>
              <tr>
                <th>发货地址</th><td colspan="3">{{ summaryDetailRows[0]?.customer_address || '-' }}</td>
                <th>汇总金额</th><td colspan="3" class="summary-amount-cell">{{ money(summaryDetailTotalAmount) }}</td>
              </tr>
            </tbody>
          </table>
          <table class="quote-preview-lines" style="margin-top:8px;">
            <thead>
              <tr>
                <th>序号</th><th>来源报价单</th><th>报价员</th><th>产品名称</th><th>材质结构</th><th>拉手颜色</th><th>宽</th><th>高</th><th>厚</th><th>数量</th><th>工艺说明</th><th>单位</th><th>总量</th><th>单价</th><th>金额</th><th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(d, i) in summaryMergedDetailRows" :key="`md_${i}_${d.quote_id}`">
                <td>{{ i + 1 }}</td>
                <td>{{ d.quote_id }}</td>
                <td>{{ d.created_by || '-' }}</td>
                <td>{{ d.product_name || '-' }}</td>
                <td>{{ d.material_structure || '-' }}</td>
                <td>{{ d.handle_color || '-' }}</td>
                <td>{{ d.width_mm || '-' }}</td>
                <td>{{ d.height_mm || '-' }}</td>
                <td>{{ d.thickness_mm || '-' }}</td>
                <td>{{ d.quantity || '-' }}</td>
                <td class="left-text">{{ d.process_desc || '-' }}</td>
                <td>{{ d.unit || '-' }}</td>
                <td>{{ d.area_m2 || '-' }}</td>
                <td>{{ d.final_unit_price || '-' }}</td>
                <td class="quote-amount">{{ money(d.amount || 0) }}</td>
                <td>{{ d.attachment_name || '-' }}</td>
              </tr>
              <tr class="quote-total-row">
                <td colspan="9">合计</td>
                <td>{{ summaryTotalQty }}</td>
                <td></td>
                <td></td>
                <td>{{ summaryTotalArea }}</td>
                <td></td>
                <td>{{ money(summaryDetailTotalAmount) }}</td>
                <td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { batchImport, calculateQuoteDetail, checkPayment, clearAuthToken, createAuthUser, createContract, createCustomerOrder, createProject, createQuoteAssignment, createQuoteRule, deleteContract, deleteFinanceVoucher, deleteMaster, deleteQuoteOrder, deleteQuoteRule, getMasterDetail, getProject, getQuoteOrder, listAuthUsers, listContractCad, listContractLogs, listContracts, listCustomerOrders, listCustomers, listFinanceVouchers, listMaterials, listProducts, listProjects, listQuoteAssignments, listQuoteDetails, listQuoteLogs, listQuoteOrders, listQuoteRules, listStaff, login, logout as apiLogout, resetAuthUserPassword, saveQuoteDetails, setAuthToken, updateAuthUser, updateContract, updateContractStatus, updateMaster, updateQuoteAssignment, updateQuoteOrder, updateQuoteRule, uploadContractCad, uploadFinanceVoucher, uploadMaterialImage, uploadMasterImageTemp, uploadProductImage } from './api/contract'
import brandLogo from './assets/lonzvine-logo.png'
import loginBg from './assets/login-factory.jpg'
import CustomerProjectProfile from './views/CustomerProjectProfile.vue'
import QuoteExtraPricePanel from './components/QuoteExtraPricePanel.vue'
import OrderFlowV3 from './views/OrderFlowV3.vue'

const C = {
  master: '基础资料', staff: '人员管理', userAuth: '账号权限', product: '产品资料', materialData: '材料资料', processRule: '工艺规则', priceRule: '价格规则',
  customer: '客户管理', order: '订单管理', quote: '报价管理', schedule: '排单管理', production: '生产管理', purchase: '采购库存', finance: '财务数据',
}

const modules = [
  { name: C.master, children: ['1.1 人员管理', '1.2 账号权限', '1.3 产品资料', '1.4 材料资料', '1.5 工艺规则', '1.6 价格规则', '1.7 生产线配置'] },
  { name: C.customer, children: ['2.1 客户档案', '2.2 跟进记录', '2.3 客户需求'] },
  { name: C.order, children: ['3.1 客户订单', '3.2 CAD评审池', '3.3 拆单', '3.4 工厂订单', '3.5 补单管理', '3.6 售后管理'] },
  { name: C.quote, children: ['4.1 报价订单池', '4.2 完整报价明细', '4.3 报价版本', '4.4 客户报价汇总', '4.5 报价PDF与客户确认'] },
  { name: C.schedule, children: ['5.1 待下料订单', '5.2 板材清单', '5.3 五金清单', '5.4 BOM清单'] },
  { name: C.production, children: ['6.1 生产任务', '6.2 排产计划', '6.3 工序进度', '6.4 质检包装'] },
  { name: C.purchase, children: ['7.1 五金资料', '7.2 库存管理', '7.3 采购申请', '7.4 采购订单', '7.5 出入库记录'] },
  { name: C.finance, children: ['8.1 付款计划', '8.2 到账确认', '8.3 价格调整审批', '8.4 下料放行', '8.5 月结与逾期', '8.6 收款账户配置'] },
]

const moduleNumbers = { [C.master]: '1', [C.customer]: '2', [C.order]: '3', [C.quote]: '4', [C.schedule]: '5', [C.production]: '6', [C.purchase]: '7', [C.finance]: '8' }
const moduleDisplayName = (name) => moduleNumbers[name] ? `${moduleNumbers[name]}. ${name}` : name
const subName = (name) => String(name || '').replace(/^\d+\.\d+\s+/, '')
const v3SubModules = new Set(['生产线配置', '客户订单', 'CAD评审池', '拆单', '工厂订单', '补单管理', '报价订单池', '完整报价明细', '报价版本', '客户报价汇总', '报价PDF与客户确认', '五金资料', '库存管理', '付款计划', '到账确认', '价格调整审批', '下料放行', '月结与逾期', '收款账户配置'])

const menuKeyword = ref('')
const sidebarCollapsed = ref(false)
const globalSearch = ref('')
const menuIcon = (name) => ({
  基础资料: '📦',
  客户管理: '👥',
  订单管理: '🧾',
  报价管理: '💬',
  排单管理: '🗂',
  生产管理: '🏭',
  采购库存: '📊',
  财务数据: '💰'
}[name] || '◻')
const isLoggedIn = ref(false)
const loginForm = ref({ username: '', password: '' })
const loginUserName = ref('')
const loginRole = ref('')
const allowedModules = ref([])
const activeModule = ref(C.master)
const activeSubModule = ref(C.staff)
const masterSearch = ref('')
const products = ref([])
const materials = ref([])
const staffList = ref([])
const authUsers = ref([])
const authUserKeyword = ref('')
const authRoleOptions = ['ADMIN', 'DIRECTOR', 'SERVICE', 'ENGINEER', 'FINANCE', 'PRODUCTION']

async function doLogin() {
  const username = String(loginForm.value.username || '').trim()
  const password = String(loginForm.value.password || '')
  if (!username) return window.alert('请输入账号')
  if (!password) return window.alert('请输入密码')
  try {
    const { data } = await login({ username, password })
    if (data.code !== 0) return window.alert(data.message || '登录失败')
    const d = data.data || {}
    setAuthToken(d.token || '')
    isLoggedIn.value = true
    loginUserName.value = d.displayName || d.username || username
    loginRole.value = d.roleCode || ''
    allowedModules.value = Array.isArray(d.allowedModules) ? d.allowedModules : []
    quoteForm.value.operator = loginUserName.value || quoteForm.value.operator
    await bootstrapAfterLogin()
  } catch (e) {
    window.alert(e?.response?.data?.message || e?.message || '登录失败')
  }
}

async function logout() {
  try { await apiLogout() } catch {}
  isLoggedIn.value = false
  loginUserName.value = ''
  loginRole.value = ''
  allowedModules.value = []
  loginForm.value.password = ''
  clearAuthToken()
}

const importVisible = ref(false)
const importModule = ref(C.staff)
const importText = ref('[]')

const filterVisible = ref(false)
const filterForm = ref({ keyword: '', status: '', color: '' })
const activeFilter = ref({ keyword: '', status: '', color: '' })

const addVisible = ref(false)
const addModule = ref(C.staff)
const statusOptions = ['暂存', '启用', '弃用']
const roleOptions = ['客服', '深化设计师', '下单员', '排产员', '车间']
const dragState = ref({ product: false, material: false })
const addPreview = ref({ product: '', material: '' })
const addStaff = ref({ staff_code: '', staff_name: '', role_type: '', process_name: '', phone: '', status: '暂存' })
const addUser = ref({ username: '', displayName: '', roleCode: '', password: '123456', enabled: 1 })
const addProduct = ref({ product_code: '', type: '', product_name: '', model: '', material_name: '', color: '', handle_color: '', unit_price: '', unit_price_unit: 'm2', thickness: '', thickness_unit: 'mm', size: '', image_url: '', unit: '平方米', status: '暂存' })
const addMaterial = ref({ material_code: '', material_name: '', material_type: '', color: '', length_mm: '', width_mm: '', thickness_mm: '', image_url: '', unit: '张', status: '暂存' })

const detailVisible = ref(false)
const detailModule = ref('')
const detailData = ref({})
const editVisible = ref(false)
const editModule = ref('')
const editId = ref(null)
const editData = ref({})
const previewVisible = ref(false)
const previewSrc = ref('')
const previewScale = ref(1)
const batchMode = ref(false)
const quoteForm = ref({ contractId: '', assignmentId: '', operator: '报价员', quoteDesc: '', category: '' })
const quoteItems = ref([])
const quoteOrderKeyword = ref('')
const quoteOrderRows = ref([])
const quoteAssignments = ref([])
const quoteAllAssignments = ref([])
const assignmentKeyword = ref('')
const assignmentForm = ref({ serviceStaff: '', engineer: '', remark: '' })
const selectedEngineer = ref('')
const orderAssignmentMode = ref('ASSIGNED')
const pendingAssignments = ref([])
const quotePoolRows = ref([])
const quotePoolKeyword = ref('')
const quotePoolLoading = ref(false)
const quotePreviewVisible = ref(false)
const quotePreviewMain = ref({})
const quotePreviewDetails = ref([])
const quoteEditingId = ref(null)
const quoteDeleteVisible = ref(false)
const quoteDeleteTarget = ref({})
const quoteDeleteCaptcha = ref('')
const quoteDeleteInput = ref('')
const inlineDeleteConfirmVisible = ref(false)
const inlineDeleteConfirmText = ref('')
const inlineDeleteConfirmAction = ref(null)
const quoteSubmitMode = ref('submit')
const selectedQuoteRowIds = ref([])
const quoteBatchDeleteMode = ref(false)
const addCategoryVisible = ref(false)
const newCategoryName = ref('')
const customQuoteCategories = ref([])
const assignmentBoardCollapsed = ref(false)
const lastQuoteContractId = ref('')
const quoteLogsVisible = ref(false)
const quoteLogsTarget = ref({})
const quoteLogsRows = ref([])
const financeConfirmVisible = ref(false)
const financeConfirmTarget = ref(null)
const financeConfirmCaptcha = ref('')
const financeConfirmInput = ref('')
const quoteSubmitConfirmVisible = ref(false)
const quoteSubmitCaptcha = ref('')
const quoteSubmitInput = ref('')
const financeCommittedMap = ref({})
const orderQuoteProgressMap = ref({})
const productCatalog = ref([])
const contractKeyword = ref('')
const contractOptions = ref([])
const customerKeyword = ref('')
const customerOptions = ref([])
const customerDropdownOpen = ref(false)
const customerProjects = ref([])
const customerOrderOptions = ref([])
const orderInlineCustomerOrderVisible = ref(false)
const orderInlineCustomerOrderForm = ref({ customerOrderNo: '', customerOrderName: '', remark: '' })
const projectDialogVisible = ref(false)
const projectDialogSource = ref('')
const projectCustomerKeyword = ref('')
const projectCustomerOptions = ref([])
const projectCustomerDropdownOpen = ref(false)
const projectForm = ref({ customerId: '', projectName: '', projectAddress: '', projectManager: '', remark: '' })
const orderInlineProjectVisible = ref(false)
const orderInlineProjectForm = ref({ projectName: '', projectAddress: '', projectManager: '', remark: '' })
const projectDetailVisible = ref(false)
const projectDetail = ref({})
const projectDetailTab = ref('overview')
const cadFiles = ref([])
const cadDrag = ref(false)
const cadPreviewVisible = ref(false)
const cadPreviewSrc = ref('')
const cadPreviewType = ref('')
const cadScale = ref(1)
const orderCreateStep = ref(1)
const masterContractForm = ref({ masterContractNo: '' })
const orderForm = ref({
  customerId: '', projectId: '', customerOrderId: '', factoryOrderPrefix: 'V', factoryOrderNo: '',
  customerName: '', customerPhone: '', customerAddress: '', demandDesc: '', owner: '',
  customerOrderNo: '', doorType: '', doorTypeRemark: '', colorName: '', colorRemark: '',
  materialName: '', coreBoardType: '', colorSeries: '', woodVeneer: '', doorThickness: '',
  boardMaterial: '', wallPanelModel: '', wallPanelMaterial: '', receiveDate: '', quoteDate: '',
  exportDate: '', workDays: '', quoterName: '', doorTechnician: '', smallPartTechnician: ''
})
const orderRows = ref([])
const orderKeyword = ref('')
const orderPendingOnly = ref(false)
const orderEditVisible = ref(false)
const orderEditId = ref(null)
const orderEditForm = ref({
  customerId: '', projectId: '', customerOrderId: '', factoryOrderNo: '', customerOrderNo: '',
  customerName: '', customerPhone: '', customerAddress: '', demandDesc: '', customFields: '',
  assignmentId: '', assignmentServiceStaff: '', assignmentEngineer: '', assignmentRemark: ''
})
const orderDetailEditVisible = ref(false)
const orderDetailEditId = ref(null)
const orderDetailEditForm = ref({
  masterContractNo: '',
  customerName: '',
  customerPhone: '',
  customerAddress: '',
  customerOrderNo: '',
  owner: '',
  demandDesc: '',
  doorType: '',
  colorName: '',
  materialName: '',
  colorSeries: '',
  doorThickness: '',
  doorTypeRemark: '',
  colorRemark: '',
  coreBoardType: '',
  woodVeneer: '',
  boardMaterial: '',
  wallPanelModel: '',
  wallPanelMaterial: '',
  quoteDate: '',
  receiveDate: '',
  exportDate: '',
  quoterName: '',
  workDays: '',
  doorTechnician: '',
  smallPartTechnician: ''
})
const orderGroupedView = ref(false)
const expandedOrderGroups = ref({})
const orderLogsVisible = ref(false)
const orderLogs = ref([])
const currentOrderNo = ref('')
const summaryDetailVisible = ref(false)
const summaryDetailRows = ref([])
const summaryDetailContractNo = ref('')
const summaryMergedDetailRows = ref([])
const quoteMainContractNo = ref('')
const financeKeyword = ref('')
const financeCheckMap = ref({})
const selectedIds = ref({
  [C.staff]: [],
  [C.product]: [],
  [C.materialData]: [],
})

const processRuleCategory = ref('柜门')
const priceRuleKeyword = ref('')
const priceRuleRows = ref([])
const ruleStatusOptions = ['暂存', '启用', '弃用']
const newPriceRule = ref({
  ruleCategory: '柜门',
  ruleName: '',
  adjustMode: 'FIXED_PER_M2',
  adjustValue: 0,
  unitDesc: '元/平',
  minAreaM2: null,
  minCharge: null,
  maxCharge: null,
  status: '启用',
  remark: ''
})
const ruleEditVisible = ref(false)
const ruleEditId = ref(null)
const ruleEditForm = ref({})
const ruleDetailVisible = ref(false)
const ruleDetailData = ref({})
const undoStack = ref([])
const undoIndex = ref(-1)
const restoringUndo = ref(false)
const quoteEditBaseline = ref('')

const filteredModules = computed(() => {
  const kw = menuKeyword.value.trim()
  if (!kw) return modules
  return modules.filter((m) => moduleDisplayName(m.name).includes(kw) || m.children.some((c) => c.includes(kw)))
})
const moduleAllowed = (name) => {
  const scope = (allowedModules.value || []).filter(Boolean)
  return scope.length === 0 || scope.includes(name)
}
const activeSubTitle = computed(() => modules.find((m) => m.name === activeModule.value)?.children.find((s) => subName(s) === activeSubModule.value) || activeSubModule.value)
const isMasterDataPage = computed(() => activeModule.value === C.master && [C.staff, C.userAuth, C.product, C.materialData].includes(activeSubModule.value))
const showFilterBtn = computed(() => !batchMode.value && activeModule.value === C.master)
const showImportBtn = computed(() => !batchMode.value && activeModule.value === C.master && [C.staff, C.product, C.materialData].includes(activeSubModule.value))
const showTemplateBtn = computed(() => showImportBtn.value)
const showAddBtn = computed(() => !batchMode.value && isMasterDataPage.value)
const showExportBtn = computed(() => !batchMode.value && (
  isMasterDataPage.value ||
  (activeModule.value === C.order && ['项目订单', '订单状态'].includes(activeSubModule.value)) ||
  (activeModule.value === C.quote && ['报价单主表', '报价明细', '深化订单池', '财务确认'].includes(activeSubModule.value))
))
const isCustomerArchivePage = computed(() => activeModule.value === C.customer && activeSubModule.value === '客户档案')
const isBatchPage = computed(() => [C.staff, C.product, C.materialData].includes(activeSubModule.value))
const quoteTotalAmount = computed(() => quoteItems.value.reduce((sum, r) => sum + Number(r.amount || 0), 0))
const quoteDateText = computed(() => new Date().toLocaleDateString('zh-CN'))
const selectedContract = computed(() => contractOptions.value.find((c) => String(c.id) === String(quoteForm.value.contractId)) || null)
const NO_SPECIFIC_PROJECT = '__NO_PROJECT__'
const NO_SPECIFIC_PROJECT_NAME = '无具体项目'
const selectedOrderProject = computed(() => customerProjects.value.find((p) => String(p.projectId) === String(orderForm.value.projectId)) || null)
const projectDetailOrders = computed(() => (orderRows.value || []).filter((r) => String(r.projectId || '') === String(projectDetail.value.projectId || '')))
const firstQuoteDetail = computed(() => quotePreviewDetails.value[0] || {})
const previewColorText = computed(() => {
  const desc = String(firstQuoteDetail.value.process_desc || '')
  const hit = desc.split('/').map((x) => x.trim()).find((x) => x && !x.includes('门') && !x.includes('板'))
  return hit || '-'
})

function isSubmittedQuoteRow(row) {
  const desc = String(row?.quote_desc || '').trim()
  return !desc.startsWith('[DRAFT]')
}

const quoteContractSummaryRows = computed(() => {
  const map = new Map()
  const doneAssignmentByContract = new Map()
  for (const row of quoteOrderRows.value.filter(isSubmittedQuoteRow)) {
    const key = row.contract_no || `ID-${row.contract_id || row.quote_id}`
    const assignmentId = Number(row.assignment_id || 0)
    if (!doneAssignmentByContract.has(key)) doneAssignmentByContract.set(key, new Set())
    if (assignmentId > 0) doneAssignmentByContract.get(key).add(assignmentId)
    if (!map.has(key)) {
      map.set(key, {
        contract_no: row.contract_no || '-',
        project_name: row.project_name || '-',
        customer_order_no: row.customer_order_no || '-',
        customer_name: row.customer_name || '-',
        quote_count: 0,
        total_amount: 0,
        engineers: new Set(),
        assignedEngineers: new Set(),
        assignTotal: 0,
        assignDone: 0
      })
    }
    const it = map.get(key)
    it.quote_count += 1
    it.total_amount += Number(row.total_amount || 0)
    if (row.engineer) it.engineers.add(row.engineer)
    else if (row.created_by) it.engineers.add(row.created_by)
  }
  for (const a of quoteAllAssignments.value || []) {
    const key = a.contract_no || `ID-${a.contract_id || a.id}`
    if (!map.has(key)) {
      map.set(key, {
        contract_no: a.contract_no || '-',
        project_name: a.project_name || '-',
        customer_order_no: a.customer_order_no || '-',
        customer_name: '-',
        quote_count: 0,
        total_amount: 0,
        engineers: new Set(),
        assignedEngineers: new Set(),
        assignTotal: 0,
        assignDone: 0
      })
    }
    const it = map.get(key)
    if (!it.project_name || it.project_name === '-') it.project_name = a.project_name || '-'
    if (!it.customer_order_no || it.customer_order_no === '-') it.customer_order_no = a.customer_order_no || '-'
    if (a.engineer) it.assignedEngineers.add(a.engineer)
    it.assignTotal += 1
    // 完成数以当前实际存在的报价单为准，避免分配统计字段延迟导致 3/3 这类误差
    const doneSet = doneAssignmentByContract.get(key)
    if (doneSet && doneSet.has(Number(a.id || 0))) it.assignDone += 1
  }
  return Array.from(map.values()).map((x) => ({
    ...x,
    engineersText: Array.from(x.assignedEngineers).join(' / ') || Array.from(x.engineers).join(' / ') || '-',
    progressText: x.assignTotal > 0 ? `${x.assignDone}/${x.assignTotal}` : '-/-',
    pendingTooltip: x.assignTotal > 0
      ? (x.assignDone >= x.assignTotal
          ? '已全部完成报价'
          : `未完成：${(quoteAllAssignments.value || [])
            .filter((a) => String(a.contract_no || `ID-${a.contract_id || a.id}`) === String(x.contract_no || ''))
            .filter((a) => {
              const doneSet = doneAssignmentByContract.get(String(x.contract_no || ''))
              return !(doneSet && doneSet.has(Number(a.id || 0)))
            })
            .map((a) => a.engineer || `分配ID:${a.id}`)
            .join('、')}`)
      : '未分配'
  }))
})

const financeRows = computed(() => {
  const kw = String(financeKeyword.value || '').trim()
  return (orderRows.value || [])
    .filter((r) => ['待财务确认', '财务已收款', '待排产'].includes(String(r.status || '')))
    .filter((r) => {
      if (!kw) return true
      return String(getFactoryOrderNo(r) || '').includes(kw) || String(getCustomerOrderNo(r) || '').includes(kw) || String(r.customerName || '').includes(kw)
    })
})
const filteredOrderRows = computed(() => {
  const kw = String(orderKeyword.value || '').trim().toLowerCase()
  const byKeyword = !kw
    ? (orderRows.value || [])
    : (orderRows.value || []).filter((r) =>
    String(r.contractNo || '').toLowerCase().includes(kw) ||
    String(r.factoryOrderNo || '').toLowerCase().includes(kw) ||
    String(r.customerOrderNo || '').toLowerCase().includes(kw) ||
    String(r.projectNo || '').toLowerCase().includes(kw) ||
    String(r.projectName || r.projectNameSnapshot || '').toLowerCase().includes(kw) ||
    String(getOrderMasterNo(r) || '').toLowerCase().includes(kw) ||
    String(r.customerName || '').toLowerCase().includes(kw) ||
    String(r.customerPhone || '').toLowerCase().includes(kw) ||
    String(r.owner || '').toLowerCase().includes(kw) ||
    String(r.status || '').toLowerCase().includes(kw)
  )
  if (!orderPendingOnly.value) return byKeyword
  return byKeyword.filter(isOrderPendingFill)
})

function isOrderPendingFill(row) {
  const c = parseOrderCustomFields(row)
  const ext = c?.子合同扩展 || {}
  const checkFields = [
    ext?.门型, ext?.颜色, ext?.材质, ext?.芯板类型, ext?.木皮,
    ext?.板材材质, ext?.墙板型号, ext?.墙板材质, ext?.报价日期,
    ext?.接单日期, ext?.出口日期, ext?.报价员, ext?.门板工艺员, ext?.小件工艺员
  ]
  return checkFields.some((x) => !String(x || '').trim())
}

const orderFormTrackedFields = computed(() => [
  orderForm.value.customerId,
  orderForm.value.projectId,
  orderForm.value.customerOrderId,
  orderForm.value.customerName,
  orderForm.value.customerPhone,
  orderForm.value.customerAddress,
  orderForm.value.customerOrderNo,
  orderForm.value.owner,
  orderForm.value.receiveDate,
  orderForm.value.demandDesc,
  assignmentForm.value.serviceStaff,
  orderAssignmentMode.value,
  orderAssignmentMode.value === 'ASSIGNED' ? selectedEngineer.value : '订单池'
])
const orderFormTotalCount = computed(() => orderFormTrackedFields.value.length)
const orderFormFilledCount = computed(() => orderFormTrackedFields.value.filter((x) => String(x || '').trim()).length)

const quotePoolFilteredRows = computed(() => {
  const kw = String(quotePoolKeyword.value || '').trim().toLowerCase()
  return (quotePoolRows.value || [])
    .filter((row) => ['待领取', 'PENDING_CLAIM'].includes(String(row.status || '')) || !String(row.engineer || '').trim())
    .filter((row) => {
      if (!kw) return true
      const order = row.order || {}
      return [
        row.contract_no,
        row.service_staff,
        row.remark,
        getFactoryOrderNo(order),
        getCustomerOrderNo(order),
        order.customerName,
        order.projectName,
        order.projectNameSnapshot,
        order.demandDesc
      ].some((v) => String(v || '').toLowerCase().includes(kw))
    })
})

const groupedOrderRows = computed(() => {
  const map = new Map()
  for (const row of filteredOrderRows.value || []) {
    const masterNo = String(getOrderMasterNo(row) || '-').trim() || '-'
    if (!map.has(masterNo)) map.set(masterNo, [])
    map.get(masterNo).push(row)
  }
  return Array.from(map.entries()).map(([masterNo, children]) => {
    const customers = Array.from(new Set(children.map((x) => String(x.customerName || '-').trim()).filter(Boolean)))
    const latestTs = children
      .map((x) => new Date(x.createdAt || 0).getTime())
      .filter((x) => Number.isFinite(x))
      .sort((a, b) => b - a)[0]
    return {
      masterNo,
      children: children.sort((a, b) => Number(b.id || 0) - Number(a.id || 0)),
      customersText: customers.join(' / ') || '-',
      latestCreatedAtText: latestTs ? formatDateTime(new Date(latestTs)) : '-'
    }
  }).sort((a, b) => String(a.masterNo).localeCompare(String(b.masterNo)))
})

function isOrderGroupExpanded(masterNo) {
  return expandedOrderGroups.value[String(masterNo)] !== false
}

function toggleOrderGroup(masterNo) {
  const key = String(masterNo)
  expandedOrderGroups.value[key] = !isOrderGroupExpanded(key)
}

function expandAllOrderGroups() {
  const next = {}
  for (const g of groupedOrderRows.value) next[String(g.masterNo)] = true
  expandedOrderGroups.value = next
}

function collapseAllOrderGroups() {
  const next = {}
  for (const g of groupedOrderRows.value) next[String(g.masterNo)] = false
  expandedOrderGroups.value = next
}

function toggleOrderGroupView() {
  orderGroupedView.value = !orderGroupedView.value
  if (orderGroupedView.value) expandAllOrderGroups()
}

function financeCategory(row) {
  const committed = financeCommittedMap.value[String(row.id)]
  if (committed?.category) return committed.category
  const expected = Number(financeExpectedAmount(row) || 0)
  if (String(row.status || '') === '待排产' || String(row.status || '') === '财务已收款') return 'full'
  if (expected <= 0) return 'unpaid'
  return 'unpaid'
}

const financeGroupedRows = computed(() => {
  const full = []
  const partial = []
  const unpaid = []
  for (const row of financeRows.value) {
    const c = financeCategory(row)
    if (c === 'full') full.push(row)
    else if (c === 'partial') partial.push(row)
    else unpaid.push(row)
  }
  return [
    { key: 'full', label: '已付全款', rows: full },
    { key: 'partial', label: '部分付款', rows: partial },
    { key: 'unpaid', label: '待付款', rows: unpaid }
  ]
})

const filteredQuoteContractSummaryRows = computed(() => {
  if (!quoteMainContractNo.value) return quoteContractSummaryRows.value
  return quoteContractSummaryRows.value.filter((r) => String(r.contract_no || '') === String(quoteMainContractNo.value))
})

const filteredQuoteOrderRows = computed(() => {
  const rows = quoteOrderRows.value.filter(isSubmittedQuoteRow)
  if (!quoteMainContractNo.value) return rows
  return rows.filter((r) => String(r.contract_no || '') === String(quoteMainContractNo.value))
})

const quoteMainInfo = computed(() => {
  if (!quoteMainContractNo.value) return null
  const base = quoteContractSummaryRows.value.find((r) => String(r.contract_no || '') === String(quoteMainContractNo.value))
  const first = filteredQuoteOrderRows.value[0] || {}
  if (!base) return null
  return {
    ...base,
    customer_phone: first.customer_phone || '-',
    customer_address: first.customer_address || '-'
  }
})

const summaryDetailInfo = computed(() =>
  quoteContractSummaryRows.value.find((r) => String(r.contract_no || '') === String(summaryDetailContractNo.value)) || null
)

const summaryDetailTotalAmount = computed(() =>
  summaryMergedDetailRows.value.reduce((sum, d) => sum + Number(d.amount || 0), 0)
)
const summaryTotalQty = computed(() =>
  summaryMergedDetailRows.value.reduce((sum, d) => sum + Number(d.quantity || 0), 0)
)
const summaryTotalArea = computed(() =>
  summaryMergedDetailRows.value.reduce((sum, d) => sum + Number(d.area_m2 || 0), 0).toFixed(4)
)
const quoteAssignProgress = computed(() => {
  const rows = quoteAssignments.value || []
  const total = rows.length
  const doneRows = rows.filter((a) => Number(a.quote_count || 0) > 0)
  const done = doneRows.length
  const pending = rows
    .filter((a) => Number(a.quote_count || 0) <= 0)
    .map((a) => a.engineer || `分配ID:${a.id}`)
  return {
    total,
    done,
    pendingTooltip: pending.length ? `未完成：${pending.join('、')}` : '已全部完成报价'
  }
})

async function openContractSummaryDetail(row) {
  const key = String(row.contract_no || '')
  summaryDetailContractNo.value = key || '-'
  summaryDetailRows.value = quoteOrderRows.value
    .filter((x) => String(x.contract_no || '') === key)
    .sort((a, b) => new Date(b.created_at || 0) - new Date(a.created_at || 0))
  const merged = []
  for (const q of summaryDetailRows.value) {
    const quoteId = q.quote_id || q.quoteId || q.id
    if (!quoteId) continue
    try {
      const resp = await listQuoteDetails(quoteId)
      const rows = resp?.data?.data || []
      for (const d of rows) {
        merged.push({ ...d, quote_id: quoteId, created_by: q.created_by || '-' })
      }
    } catch (e) {
      console.warn('load quote details failed', quoteId, e)
    }
  }
  summaryMergedDetailRows.value = merged
  summaryDetailVisible.value = true
}

async function exportSummaryDetailExcel() {
  if (!summaryDetailContractNo.value) return window.alert('请先打开订单总明细')
  const filename = `${summaryDetailContractNo.value}_总明细汇总.xls`
  const info = summaryDetailInfo.value || {}
  const logo = await getLogoDataUrl()
  const logoHtml = logo ? `<div style="margin:0 0 8px;text-align:left;"><img src="${logo}" alt="龙泽伟尼" style="height:42px;" /></div>` : ''
  const html = `
    <html><head><meta charset="utf-8" />
    <style>table{border-collapse:collapse;width:100%;font-size:12px;}th,td{border:1px solid #999;padding:6px;text-align:center;}th{background:#f3f4f6;}h3{margin:4px 0 8px;}</style>
    </head><body>
    ${logoHtml}
    <h3>订单总明细 - ${esc(summaryDetailContractNo.value)}</h3>
    <table><tr><th>客户名称</th><th>参与工程师</th><th>报价份数</th><th>汇总金额</th></tr>
    <tr><td>${esc(info.customer_name || '-')}</td><td>${esc(info.engineersText || '-')}</td><td>${esc(info.quote_count || 0)}</td><td>${esc(money(summaryDetailTotalAmount.value))}</td></tr></table>
    <br/>
    <table>
      <tr><th>来源报价单</th><th>报价员</th><th>产品名称</th><th>材质结构</th><th>拉手颜色</th><th>宽</th><th>高</th><th>厚</th><th>数量</th><th>单位</th><th>面积</th><th>单价</th><th>金额</th><th>工艺说明</th></tr>
      ${summaryMergedDetailRows.value.map((d) => `<tr><td>${esc(d.quote_id)}</td><td>${esc(d.created_by || '-')}</td><td>${esc(d.product_name || '-')}</td><td>${esc(d.material_structure || '-')}</td><td>${esc(d.handle_color || '-')}</td><td>${esc(d.width_mm || '-')}</td><td>${esc(d.height_mm || '-')}</td><td>${esc(d.thickness_mm || '-')}</td><td>${esc(d.quantity || '-')}</td><td>${esc(d.unit || '-')}</td><td>${esc(d.area_m2 || '-')}</td><td>${esc(d.final_unit_price || '-')}</td><td>${esc(money(d.amount || 0))}</td><td>${esc(d.process_desc || '-')}</td></tr>`).join('')}
    </table>
    </body></html>`
  const blob = new Blob([`\uFEFF${html}`], { type: 'application/vnd.ms-excel;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
const quoteCategoryOptions = computed(() => {
  return uniqueText([
    ...productCatalog.value.map((p) => getProductCategory(p)),
    ...customQuoteCategories.value
  ])
})
const isAllQuoteRowsSelected = computed(() => quoteItems.value.length > 0 && quoteItems.value.every((r) => selectedQuoteRowIds.value.includes(r.uid)))
const engineerOptions = computed(() => {
  const list = staffList.value || []
  const deepening = list.filter((s) => String(s.role_type || '').includes('深化'))
  return (deepening.length ? deepening : list).filter((s) => String(s.staff_name || '').trim())
})
const ownerStaffOptions = computed(() => (staffList.value || []).filter((s) => String(s.staff_name || '').trim()))
const ORDER_FLOW = {
  报价中: ['待客户确认'],
  待客户确认: ['待财务确认'],
  待财务确认: ['财务已收款'],
  财务已收款: ['待排产'],
  待排产: ['生产中'],
  生产中: ['已完成'],
  已完成: ['待发货'],
  待发货: ['已发货'],
  已发货: []
}

const staffRows = computed(() => applyClientFilter(staffList.value, activeFilter.value, ['staff_code', 'staff_name', 'role_type', 'process_name'], ['status']))
const productRows = computed(() => applyClientFilter(products.value, activeFilter.value, ['product_code', 'type', 'product_name', 'model', 'material_name', 'color', 'handle_color', 'size'], ['status'], 'color'))
const materialRows = computed(() => applyClientFilter(materials.value, activeFilter.value, ['material_code', 'material_name', 'material_type', 'color'], ['status'], 'color'))
const currentQuoteEditFingerprint = computed(() => JSON.stringify({
  quoteForm: quoteForm.value,
  quoteItems: quoteItems.value,
  assignmentForm: assignmentForm.value,
  pendingAssignments: pendingAssignments.value
}))
const isQuoteEditDirty = computed(() => Boolean(
  quoteEditBaseline.value &&
  currentQuoteEditFingerprint.value !== quoteEditBaseline.value
))
const isAdvancedUser = computed(() => String(loginRole.value || '').toUpperCase() === 'ADMIN')
let confirmedLeaveFingerprint = ''

function confirmLeaveEditIfNeeded() {
  if (!isQuoteEditDirty.value) return true
  const fp = currentQuoteEditFingerprint.value
  if (confirmedLeaveFingerprint && confirmedLeaveFingerprint === fp) return true
  const ok = window.confirm('当前报价单有未保存修改，离开后会丢失。是否继续离开？')
  if (ok) {
    // For this exact unsaved version, allow leaving without repeated prompts.
    confirmedLeaveFingerprint = fp
  }
  return ok
}

function buildUndoSnapshot() {
  return JSON.stringify({
    quoteForm: quoteForm.value,
    quoteItems: quoteItems.value,
    assignmentForm: assignmentForm.value,
    pendingAssignments: pendingAssignments.value,
    selectedEngineer: selectedEngineer.value,
    orderForm: orderForm.value,
    addStaff: addStaff.value,
    addProduct: addProduct.value,
    addMaterial: addMaterial.value,
    editData: editData.value,
    financeCheckMap: financeCheckMap.value
  })
}

function applyUndoSnapshot(snapshotText) {
  const data = JSON.parse(snapshotText)
  quoteForm.value = data.quoteForm || quoteForm.value
  quoteItems.value = data.quoteItems || quoteItems.value
  assignmentForm.value = data.assignmentForm || assignmentForm.value
  pendingAssignments.value = data.pendingAssignments || pendingAssignments.value
  selectedEngineer.value = data.selectedEngineer || ''
  orderForm.value = data.orderForm || orderForm.value
  addStaff.value = data.addStaff || addStaff.value
  addProduct.value = data.addProduct || addProduct.value
  addMaterial.value = data.addMaterial || addMaterial.value
  editData.value = data.editData || editData.value
  financeCheckMap.value = data.financeCheckMap || financeCheckMap.value
}

function pushUndoSnapshot() {
  if (restoringUndo.value) return
  const snap = buildUndoSnapshot()
  if (undoIndex.value >= 0 && undoStack.value[undoIndex.value] === snap) return
  if (undoIndex.value < undoStack.value.length - 1) {
    undoStack.value = undoStack.value.slice(0, undoIndex.value + 1)
  }
  undoStack.value.push(snap)
  if (undoStack.value.length > 80) undoStack.value.shift()
  undoIndex.value = undoStack.value.length - 1
}

function undoLastChange() {
  if (undoIndex.value <= 0) return
  undoIndex.value -= 1
  restoringUndo.value = true
  try {
    applyUndoSnapshot(undoStack.value[undoIndex.value])
  } finally {
    restoringUndo.value = false
  }
}

function redoLastChange() {
  if (undoIndex.value >= undoStack.value.length - 1) return
  undoIndex.value += 1
  restoringUndo.value = true
  try {
    applyUndoSnapshot(undoStack.value[undoIndex.value])
  } finally {
    restoringUndo.value = false
  }
}

function onGlobalKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && String(e.key || '').toLowerCase() === 'z' && !e.shiftKey) {
    e.preventDefault()
    undoLastChange()
    return
  }
  if ((e.ctrlKey || e.metaKey) && String(e.key || '').toLowerCase() === 'y') {
    e.preventDefault()
    redoLastChange()
    return
  }
  if ((e.ctrlKey || e.metaKey) && String(e.key || '').toLowerCase() === 'z' && e.shiftKey) {
    e.preventDefault()
    redoLastChange()
  }
}

function onBeforeUnload(e) {
  if (!isQuoteEditDirty.value) return
  e.preventDefault()
  e.returnValue = ''
}

function applyGlobalSearch() {
  const kw = String(globalSearch.value || '').trim()
  if (activeModule.value === C.master) {
    masterSearch.value = kw
    refreshMasterData()
    return
  }
  if (activeModule.value === C.order) {
    orderKeyword.value = kw
    if (!orderRows.value.length) loadOrders()
    return
  }
  if (activeModule.value === C.quote) {
    if (activeSubModule.value === '报价明细') {
      contractKeyword.value = kw
      searchContracts()
      return
    }
    quoteOrderKeyword.value = kw
    loadQuoteOrders()
  }
}

async function bootstrapAfterLogin() {
  await refreshMasterData()
  await loadAuthUsers()
  await loadOrders()
  await ensureProductCatalog()
  ensureQuoteCategory()
  await loadPriceRules()
  await loadQuoteOrders()
  fillImportExample()
  if (quoteItems.value.length === 0) addQuoteItem()
  await searchContracts()
  pushUndoSnapshot()
}

function switchModule(moduleName) {
  if (!moduleAllowed(moduleName)) return
  if (!confirmLeaveEditIfNeeded()) return
  exitBatchMode()
  activeModule.value = moduleName
  const first = subName(modules.find((m) => m.name === moduleName)?.children[0] || '')
  activeSubModule.value = first
  resetSubModuleState(first)
  refreshSubModuleData(first)
  if ([C.staff, C.product, C.materialData].includes(first)) importModule.value = first
}

function switchSubModule(subName) {
  if (!confirmLeaveEditIfNeeded()) return
  exitBatchMode()
  activeSubModule.value = subName
  resetSubModuleState(subName)
  refreshSubModuleData(subName)
}

function resetSubModuleState(subName) {
  globalSearch.value = ''
  if ([C.staff, C.product, C.materialData].includes(subName)) importModule.value = subName
  if ([C.staff, C.product, C.materialData].includes(subName)) {
    activeFilter.value = { keyword: '', status: '', color: '' }
    filterForm.value = { keyword: '', status: '', color: '' }
  }
  if (subName === C.userAuth) {
    authUserKeyword.value = ''
  }
  if (subName === '报价明细') {
    quoteEditingId.value = null
    quoteEditBaseline.value = ''
    quoteForm.value = { contractId: '', assignmentId: '', operator: loginUserName.value || '报价员', quoteDesc: '', category: '' }
    quoteItems.value = [newQuoteItem()]
    contractKeyword.value = ''
    assignmentKeyword.value = ''
    assignmentForm.value = { serviceStaff: '', engineer: '', remark: '' }
    selectedEngineer.value = ''
    pendingAssignments.value = []
    quoteBatchDeleteMode.value = false
    selectedQuoteRowIds.value = []
  }
  if (subName === '深化订单池') {
    quotePoolKeyword.value = ''
  }
  if (subName === '报价单主表') {
    quoteMainContractNo.value = ''
    quoteOrderKeyword.value = ''
  }
  if (activeModule.value === C.order && subName === '订单状态') {
    orderKeyword.value = ''
  }
}

function refreshSubModuleData(subName) {
  if ([C.staff, C.product, C.materialData].includes(subName)) {
    refreshMasterData()
  }
  if (subName === C.userAuth) {
    loadAuthUsers()
  }
  if (subName === '报价明细') {
    searchContracts()
    loadCadFiles()
    ensureProductCatalog()
    loadPriceRules()
    loadQuoteAssignments()
    if (!quoteEditBaseline.value) quoteEditBaseline.value = currentQuoteEditFingerprint.value
  }
  if (subName === '报价单主表') {
    loadQuoteOrders()
  }
  if (subName === '深化订单池') {
    loadQuotePool()
  }
  if (subName === C.priceRule) {
    loadPriceRules()
  }
  if (activeModule.value === C.order && subName === '订单状态') loadOrders()
}

function showProcessRuleNotice() {
  window.alert('工艺规则暂时只做工艺资料维护，报价加价请在价格规则中维护。')
}

async function refreshMasterData() {
  const [p, m, s] = await Promise.all([listProducts(masterSearch.value), listMaterials(masterSearch.value), listStaff(masterSearch.value)])
  products.value = p.data.data || []
  materials.value = m.data.data || []
  staffList.value = s.data.data || []
  if (!masterSearch.value) {
    productCatalog.value = products.value.filter((x) => String(x.status || '') === '启用')
    ensureQuoteCategory()
  }
}

async function loadAuthUsers() {
  if (String(loginRole.value || '').toUpperCase() !== 'ADMIN') {
    authUsers.value = []
    return
  }
  const { data } = await listAuthUsers(authUserKeyword.value)
  if (data.code !== 0) return window.alert(data.message || '加载账号失败')
  authUsers.value = data.data || []
}

function openAuthUserEdit(row) {
  editModule.value = C.userAuth
  editId.value = row.id
  editData.value = {
    username: row.username,
    display_name: row.display_name,
    role_code: row.role_code,
    enabled: Number(row.enabled) === 1 ? 1 : 0
  }
  editVisible.value = true
}

async function resetAuthPwd(row) {
  const pwd = String(window.prompt(`请输入账号 ${row.username} 的新密码（至少6位）`, '123456') || '').trim()
  if (!pwd) return
  if (pwd.length < 6) return window.alert('密码长度不能小于6位')
  const { data } = await resetAuthUserPassword(row.id, pwd)
  if (data.code !== 0) return window.alert(data.message || '重置密码失败')
  window.alert('密码已重置')
}

function openImport() {
  if (![C.staff, C.product, C.materialData].includes(activeSubModule.value)) {
    window.alert('请先切换到基础资料下的 人员管理/产品资料/材料资料 页面再导入。')
    return
  }
  importModule.value = activeSubModule.value
  fillImportExample()
  importVisible.value = true
}

function openFilter() {
  if (activeModule.value !== C.master) {
    window.alert('筛选暂时只开放给基础资料模块。')
    return
  }
  filterVisible.value = true
}

function applyFilter() {
  activeFilter.value = { ...filterForm.value }
  filterVisible.value = false
}

function resetFilter() {
  filterForm.value = { keyword: '', status: '', color: '' }
  activeFilter.value = { keyword: '', status: '', color: '' }
}

function openAdd() {
  if (![C.staff, C.userAuth, C.product, C.materialData].includes(activeSubModule.value)) {
    window.alert('请在基础资料的对应页面新增数据，例如人员管理页只能新增人员。')
    return
  }
  resetAddForms()
  addModule.value = activeSubModule.value
  addVisible.value = true
}

async function submitAdd() {
  let moduleName = ''
  let row = null

  if (addModule.value === C.staff) {
    moduleName = C.staff
    row = { ...addStaff.value }
    if (!row.staff_code || !row.staff_name || !row.role_type) return window.alert('请填写人员编码、姓名、角色')
    if (staffList.value.some((x) => String(x.staff_code || '').trim() === String(row.staff_code || '').trim())) {
      return window.alert(`人员编码重复：${row.staff_code}`)
    }
  } else if (addModule.value === C.userAuth) {
    if (String(loginRole.value || '').toUpperCase() !== 'ADMIN') return window.alert('仅管理员可新增账号')
    const username = String(addUser.value.username || '').trim()
    const displayName = String(addUser.value.displayName || '').trim()
    const roleCode = String(addUser.value.roleCode || '').trim()
    if (!username || !displayName || !roleCode) return window.alert('请填写账号、姓名、角色')
    const payload = { username, displayName, roleCode, enabled: Number(addUser.value.enabled) === 1 ? 1 : 0, password: String(addUser.value.password || '').trim() || '123456' }
    const { data } = await createAuthUser(payload)
    if (data.code !== 0) return window.alert(data.message || '新增账号失败')
    addVisible.value = false
    resetAddForms()
    await loadAuthUsers()
    return window.alert('账号新增成功，初始密码：123456')
  } else if (addModule.value === C.product) {
    moduleName = C.product
    row = { ...addProduct.value }
    if (!row.product_code || !row.product_name) return window.alert('请填写产品编码、产品风格')
    if (products.value.some((x) => String(x.product_code || '').trim() === String(row.product_code || '').trim())) {
      return window.alert(`产品编码重复：${row.product_code}`)
    }
    row.unit_price = row.unit_price === '' ? null : Number(row.unit_price)
  } else if (addModule.value === C.materialData) {
    moduleName = C.materialData
    row = { ...addMaterial.value }
    if (!row.material_code || !row.material_name) return window.alert('请填写材料编码、材料名称')
    if (materials.value.some((x) => String(x.material_code || '').trim() === String(row.material_code || '').trim())) {
      return window.alert(`材料编码重复：${row.material_code}`)
    }
    row.length_mm = row.length_mm === '' ? null : Number(row.length_mm)
    row.width_mm = row.width_mm === '' ? null : Number(row.width_mm)
    row.thickness_mm = row.thickness_mm === '' ? null : Number(row.thickness_mm)
  } else {
    return window.alert('当前页面暂未接入新增接口。')
  }

  try {
    await batchImport(moduleName, [row])
    addVisible.value = false
    resetAddForms()
    await refreshMasterData()
    if (moduleName === C.product) await refreshProductCatalog()
    window.alert('新增成功')
  } catch {
    window.alert('新增失败，请检查字段是否重复或格式不正确')
  }
}

async function viewDetail(module, id) {
  const { data } = await getMasterDetail(module, id)
  if (data.code !== 0) return window.alert(data.message)
  detailModule.value = module
  detailData.value = data.data || {}
  detailVisible.value = true
}

function openImagePreview(src) {
  previewSrc.value = src
  previewScale.value = 1
  previewVisible.value = true
}

function closeImagePreview() {
  previewVisible.value = false
  previewScale.value = 1
}

function onPreviewWheel(evt) {
  const step = evt.deltaY < 0 ? 0.1 : -0.1
  const next = Number((previewScale.value + step).toFixed(2))
  previewScale.value = Math.min(5, Math.max(0.2, next))
}

async function removeDetailImage() {
  if (!detailModule.value || !detailData.value?.id) return
  const { data } = await updateMaster(detailModule.value, detailData.value.id, { image_url: '' })
  if (data.code !== 0) return window.alert(data.message)
  detailData.value.image_url = ''
  await refreshMasterData()
}

function openEdit(module, row) {
  editModule.value = module
  editId.value = row.id
  editData.value = { ...row }
  editVisible.value = true
}

async function submitEdit() {
  if (editModule.value === C.userAuth) {
    const payload = {
      username: editData.value.username,
      displayName: editData.value.display_name,
      roleCode: editData.value.role_code,
      enabled: Number(editData.value.enabled) === 1 ? 1 : 0
    }
    const { data } = await updateAuthUser(editId.value, payload)
    if (data.code !== 0) return window.alert(data.message || '账号更新失败')
    editVisible.value = false
    await loadAuthUsers()
    return window.alert('账号更新成功')
  }
  const payload = { ...editData.value }
  delete payload.id
  delete payload.created_at
  delete payload.updated_at
  const { data } = await updateMaster(editModule.value, editId.value, payload)
  if (data.code !== 0) return window.alert(data.message)
  editVisible.value = false
  await refreshMasterData()
  if (editModule.value === C.product) await refreshProductCatalog()
  window.alert('编辑成功')
}

async function removeRow(module, id) {
  if (!window.confirm('确认删除该记录吗？')) return
  const { data } = await deleteMaster(module, id)
  if (data.code !== 0) return window.alert(data.message)
  await refreshMasterData()
  if (module === C.product) await refreshProductCatalog()
  window.alert('删除成功')
}

function currentRowsByModule(module) {
  if (module === C.staff) return staffRows.value
  if (module === C.product) return productRows.value
  if (module === C.materialData) return materialRows.value
  return []
}

function isRowSelected(module, id) {
  return selectedIds.value[module]?.includes(id)
}

function toggleRowSelect(module, id, evt) {
  const checked = !!evt?.target?.checked
  const oldList = selectedIds.value[module] || []
  const set = new Set(oldList)
  if (checked) set.add(id)
  else set.delete(id)
  selectedIds.value[module] = Array.from(set)
}

function isAllSelected(module, rows) {
  if (!rows || rows.length === 0) return false
  const set = new Set(selectedIds.value[module] || [])
  return rows.every((r) => set.has(r.id))
}

function toggleSelectAll(module, rows, evt) {
  const checked = !!evt?.target?.checked
  if (!checked) {
    selectedIds.value[module] = []
    return
  }
  selectedIds.value[module] = (rows || []).map((r) => r.id)
}

function clearCurrentSelection() {
  const module = activeSubModule.value
  if (selectedIds.value[module]) selectedIds.value[module] = []
}

async function enterBatchMode() {
  batchMode.value = true
  clearCurrentSelection()
  await refreshMasterData()
}

function exitBatchMode() {
  batchMode.value = false
  clearCurrentSelection()
}

async function batchSetStatus(status) {
  if (!batchMode.value) return
  const module = activeSubModule.value
  const ids = selectedIds.value[module] || []
  if (ids.length === 0) return window.alert('请先勾选要操作的数据')
  const tasks = ids.map((id) => updateMaster(module, id, { status }))
  const results = await Promise.allSettled(tasks)
  const ok = results.filter((r) => r.status === 'fulfilled' && r.value?.data?.code === 0).length
  const fail = ids.length - ok
  await refreshMasterData()
  if (module === C.product) await refreshProductCatalog()
  clearCurrentSelection()
  window.alert(`批量${status}完成：成功 ${ok} 条，失败 ${fail} 条`)
}

async function batchDelete() {
  if (!batchMode.value) return
  const module = activeSubModule.value
  const ids = selectedIds.value[module] || []
  if (ids.length === 0) return window.alert('请先勾选要删除的数据')
  if (!window.confirm(`确认批量删除 ${ids.length} 条记录吗？`)) return
  const tasks = ids.map((id) => deleteMaster(module, id))
  const results = await Promise.allSettled(tasks)
  const ok = results.filter((r) => r.status === 'fulfilled' && r.value?.data?.code === 0).length
  const fail = ids.length - ok
  await refreshMasterData()
  if (module === C.product) await refreshProductCatalog()
  clearCurrentSelection()
  window.alert(`批量删除完成：成功 ${ok} 条，失败 ${fail} 条`)
  exitBatchMode()
}

function resetAddForms() {
  if (addPreview.value.product) URL.revokeObjectURL(addPreview.value.product)
  if (addPreview.value.material) URL.revokeObjectURL(addPreview.value.material)
  addPreview.value = { product: '', material: '' }
  addStaff.value = { staff_code: '', staff_name: '', role_type: '', process_name: '', phone: '', status: '暂存' }
  addUser.value = { username: '', displayName: '', roleCode: '', password: '123456', enabled: 1 }
  addProduct.value = { product_code: '', type: '', product_name: '', model: '', material_name: '', color: '', handle_color: '', unit_price: '', unit_price_unit: 'm2', thickness: '', thickness_unit: 'mm', size: '', image_url: '', unit: '平方米', status: '暂存' }
  addMaterial.value = { material_code: '', material_name: '', material_type: '', color: '', length_mm: '', width_mm: '', thickness_mm: '', image_url: '', unit: '张', status: '暂存' }
}

function onAddFileChange(module, evt) {
  const file = evt.target.files?.[0]
  evt.target.value = ''
  onAddImageUpload(module, file)
}

async function onAddImageUpload(module, file) {
  if (!file) return
  if (!String(file.type || '').startsWith('image/')) {
    window.alert('请选择图片文件')
    return
  }
  if (addPreview.value[module]) {
    URL.revokeObjectURL(addPreview.value[module])
  }
  addPreview.value[module] = URL.createObjectURL(file)
  try {
    const { data } = await uploadMasterImageTemp(module, file)
    if (data.code !== 0) return window.alert(data.message || '上传失败')
    if (module === 'product') addProduct.value.image_url = data.data
    if (module === 'material') addMaterial.value.image_url = data.data
    window.alert('图片上传成功')
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '图片上传失败'
    window.alert(msg)
  }
}

async function onDropAddImage(module, evt) {
  dragState.value[module] = false
  const file = getImageFileFromDrop(evt)
  if (!file) return
  await onAddImageUpload(module, file)
}

async function onPasteAddImage(module, evt) {
  const file = getImageFileFromPaste(evt)
  if (!file) return
  await onAddImageUpload(module, file)
}

function clearAddImage(module) {
  if (addPreview.value[module]) {
    URL.revokeObjectURL(addPreview.value[module])
    addPreview.value[module] = ''
  }
  if (module === 'product') addProduct.value.image_url = ''
  if (module === 'material') addMaterial.value.image_url = ''
}

function fieldLabel(key) {
  const map = {
    username: '账号', display_name: '姓名', role_code: '角色', enabled: '状态',
    staff_code: '人员编码', staff_name: '姓名', role_type: '角色', process_name: '工序', phone: '电话',
    product_code: '产品编码', type: '产品类型', product_name: '产品风格', model: '产品名称', material_name: '材质', color: '颜色', handle_color: '拉手颜色',
    unit_price: '单价', unit_price_unit: '单价单位', thickness: '厚度', thickness_unit: '厚度单位', size: '尺寸', image_url: '图片地址', unit: '单位', status: '状态',
    material_code: '材料编码', material_type: '材质类型', length_mm: '长(mm)', width_mm: '宽(mm)', thickness_mm: '厚(mm)',
    custom_fields: '备注', custom_text1: '自定义1', custom_text2: '自定义2', custom_text3: '自定义3'
  }
  return map[key] || key
}

function statusClass(status) {
  const v = String(status || '暂存')
  if (v.includes('启用')) return 'status-enabled'
  if (v.includes('弃用')) return 'status-disabled'
  return 'status-draft'
}

function displayEmpty(value) {
  const text = String(value ?? '').trim()
  if (!text || text.toUpperCase() === 'NULL') return '-'
  return text
}

function ruleStatusText(enabled) {
  const n = Number(enabled)
  if (n === 1) return '启用'
  if (n === 2) return '暂存'
  return '弃用'
}

function ruleStatusValue(status) {
  if (status === '启用') return 1
  if (status === '暂存') return 2
  return 0
}

function ruleStatusClass(enabled) {
  return statusClass(ruleStatusText(enabled))
}

async function onEditImageUpload(evt) {
  const file = evt.target.files?.[0]
  evt.target.value = ''
  if (!file) return
  const module = editModule.value === C.product ? 'product' : 'material'
  try {
    const { data } = await uploadMasterImageTemp(module, file)
    if (data.code !== 0) return window.alert(data.message || '上传失败')
    editData.value.image_url = data.data
  } catch (e) {
    window.alert(e?.response?.data?.message || '上传失败')
  }
}

async function onDropEditImage(evt) {
  const file = getImageFileFromDrop(evt)
  if (!file) return
  await uploadImageToEdit(file)
}

async function onPasteEditImage(evt) {
  const file = getImageFileFromPaste(evt)
  if (!file) return
  await uploadImageToEdit(file)
}

async function uploadImageToEdit(file) {
  const module = editModule.value === C.product ? 'product' : 'material'
  try {
    const { data } = await uploadMasterImageTemp(module, file)
    if (data.code !== 0) return window.alert(data.message || '上传失败')
    editData.value.image_url = data.data
  } catch (e) {
    window.alert(e?.response?.data?.message || '上传失败')
  }
}

function fillImportExample() {
  const examples = {
    [C.staff]: [
      { staff_code: 'YG001', staff_name: '张客服', role_type: '客服', process_name: '', phone: '13800000000', status: '在职' },
      { staff_code: 'YG002', staff_name: '李师傅', role_type: '车间', process_name: '封边', phone: '13900000000', status: '在职' },
    ],
    [C.product]: [
      { product_code: 'CP001', product_name: '衣柜门板', model: 'YG-MB-01', material_name: '无醛板', color: '暖白', unit_price: 188, unit_price_unit: '元/m²', thickness: '18', thickness_unit: 'mm', image_url: '/api/master/files/demo_product.jpg', unit: '平方米', status: '启用' },
    ],
    [C.materialData]: [
      { material_code: 'CL001', material_name: '芦花板', material_type: '板材', color: '浅胡桃', length_mm: 2440, width_mm: 1220, thickness_mm: 18, image_url: '/api/master/files/demo_material.jpg', unit: '张', status: '启用' },
    ],
  }
  importText.value = JSON.stringify(examples[importModule.value] || [], null, 2)
}

async function onBatchImport() {
  try {
    const rows = JSON.parse(importText.value)
    const { data } = await batchImport(importModule.value, rows)
    const ok = data?.data?.success ?? 0
    const fail = data?.data?.failed ?? 0
    window.alert(`导入完成：成功 ${ok} 条，失败 ${fail} 条`)
    await refreshMasterData()
    if (importModule.value === C.product) await refreshProductCatalog()
  } catch {
    window.alert('导入失败：请确认JSON格式或后端服务状态。')
  }
}

async function onCsvImport(file) {
  if (!file) return
  const text = await file.text()
  const rows = parseCsv(text)
  importText.value = JSON.stringify(rows, null, 2)
}

function parseCsv(text) {
  const lines = text.replace(/^\uFEFF/, '').split(/\r?\n/).filter((line) => line.trim())
  if (lines.length < 2) return []
  const headers = splitCsvLine(lines[0]).map((h) => h.trim())
  return lines.slice(1).map((line) => {
    const values = splitCsvLine(line)
    return headers.reduce((row, key, index) => {
      row[key] = values[index] ?? ''
      return row
    }, {})
  })
}

function splitCsvLine(line) {
  const result = []
  let cell = ''
  let quoted = false
  for (let i = 0; i < line.length; i++) {
    const ch = line[i]
    const next = line[i + 1]
    if (ch === '"' && quoted && next === '"') { cell += '"'; i++ }
    else if (ch === '"') quoted = !quoted
    else if (ch === ',' && !quoted) { result.push(cell); cell = '' }
    else cell += ch
  }
  result.push(cell)
  return result
}

function newQuoteItem() {
  return {
    uid: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    category: '',
    productCode: '',
    productModel: '',
    productType: '',
    modelKeyword: '',
    modelOptions: [],
    productName: '',
    materialStructure: '',
    color: '',
    handleColor: '',
    thickness: '',
    thicknessUnit: 'mm',
    remark: '',
    widthMm: 0,
    heightMm: 0,
    thicknessMm: 18,
    quantity: 1,
    hingeHole: '',
    processDesc: '',
    attachmentName: '',
    unit: 'm²',
    unitPriceUnit: '元/m²',
    areaM2: 0,
    baseUnitPrice: 0,
    amount: 0,
    specialAdjustTotal: 0,
    finalUnitPrice: 0,
    appliedRules: [],
    formulaText: '',
    nonStandard: false,
    ruleKeyword: '',
    ruleOptions: [],
    selectedRuleIds: [],
    extraPanelCollapsed: false,
    customRule: { ruleName: '', adjustMode: 'FIXED_PER_M2', adjustValue: 0, unitDesc: '元/平', minAreaM2: null, ruleQuantity: null, minCharge: null, maxCharge: null, remark: '' },
    customRules: [],
  }
}

function quoteItemFromSavedDetail(d) {
  const savedProductName = String(d.product_name || '').trim()
  const savedProcessParts = String(d.process_desc || '').split('/').map((x) => x.trim()).filter(Boolean)
  const matchedProduct = productCatalog.value.find((p) => String(p.model || '').trim() === savedProductName)
    || productCatalog.value.find((p) => String(p.product_name || '').trim() === savedProductName)
  const style = matchedProduct?.product_name || savedProcessParts[0] || ''
  const productName = matchedProduct?.model || savedProductName
  const savedExtraRules = savedExtraRulesForDetail(d)
  return {
    ...newQuoteItem(),
    category: getProductCategory(matchedProduct) || quoteForm.value.category || defaultQuoteCategory(),
    productType: style,
    modelKeyword: productName,
    productName,
    materialStructure: d.material_structure || '',
    color: matchedProduct?.color || savedProcessParts[1] || '',
    handleColor: d.handle_color || matchedProduct?.handle_color || '',
    widthMm: Number(d.width_mm || 0),
    heightMm: Number(d.height_mm || 0),
    thicknessMm: Number(d.thickness_mm || 0),
    quantity: Number(d.quantity || 1),
    hingeHole: d.hinge_hole || '',
    processDesc: d.process_desc || '',
    attachmentName: d.attachment_name || '',
    unit: d.unit || 'm²',
    areaM2: Number(d.area_m2 || 0),
    baseUnitPrice: Number(d.base_unit_price || 0),
    specialAdjustTotal: Number(d.special_adjust_total || 0),
    finalUnitPrice: Number(d.final_unit_price || 0),
    amount: Number(d.amount || 0),
    remark: savedProcessParts.slice(2).join(' / ') || '',
    nonStandard: savedExtraRules.length > 0 || Number(d.special_adjust_total || 0) > 0,
    customRules: savedExtraRules,
    extraPanelCollapsed: savedExtraRules.length > 0,
    formulaText: quoteFormulaForSaved(d),
  }
}

function savedExtraRulesForDetail(detail) {
  const extras = Array.isArray(detail.extra_prices) ? detail.extra_prices : []
  if (extras.length) {
    return extras.map((x) => ({
      sourceRuleId: x.source_rule_id || null,
      ruleName: x.rule_name || '',
      adjustMode: x.adjust_mode || 'FIXED_PER_M2',
      adjustValue: Number(x.adjust_value || 0),
      unitDesc: x.unit_desc || '',
      minAreaM2: x.min_area_m2 ?? null,
      ruleQuantity: x.rule_quantity ?? null,
      minCharge: x.min_charge ?? null,
      maxCharge: x.max_charge ?? null,
      remark: x.remark || ''
    }))
  }
  try {
    const raw = detail.custom_rule_json
    const parsed = raw ? (typeof raw === 'string' ? JSON.parse(raw) : raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function addQuoteItem(preset = {}) {
  const row = newQuoteItem()
  if (preset && typeof preset === 'object' && preset.category !== undefined) {
    row.category = String(preset.category || '').trim()
  }
  quoteItems.value.push(row)
}

function removeQuoteItem(uid) {
  quoteItems.value = quoteItems.value.filter((r) => r.uid !== uid)
  selectedQuoteRowIds.value = selectedQuoteRowIds.value.filter((id) => id !== uid)
  if (!quoteItems.value.length) quoteBatchDeleteMode.value = false
}

function openInlineDeleteConfirm(text, onConfirm) {
  inlineDeleteConfirmText.value = text
  inlineDeleteConfirmAction.value = onConfirm
  inlineDeleteConfirmVisible.value = true
}

function closeInlineDeleteConfirm() {
  inlineDeleteConfirmVisible.value = false
  inlineDeleteConfirmText.value = ''
  inlineDeleteConfirmAction.value = null
}

function confirmInlineDelete() {
  const action = inlineDeleteConfirmAction.value
  closeInlineDeleteConfirm()
  if (typeof action === 'function') action()
}

function requestRemoveQuoteItem(uid) {
  openInlineDeleteConfirm('确认删除该报价明细行吗？', () => removeQuoteItem(uid))
}

function toggleSelectQuoteRow(uid, evt) {
  const checked = !!evt?.target?.checked
  const set = new Set(selectedQuoteRowIds.value)
  if (checked) set.add(uid)
  else set.delete(uid)
  selectedQuoteRowIds.value = Array.from(set)
}

function toggleSelectAllQuoteRows(evt) {
  const checked = !!evt?.target?.checked
  if (!checked) {
    selectedQuoteRowIds.value = []
    return
  }
  selectedQuoteRowIds.value = quoteItems.value.map((r) => r.uid)
}

function requestBatchRemoveQuoteItems() {
  const ids = selectedQuoteRowIds.value
  if (!ids.length) return
  openInlineDeleteConfirm(`确认批量删除 ${ids.length} 条报价明细吗？`, () => {
    const removeSet = new Set(ids)
    quoteItems.value = quoteItems.value.filter((r) => !removeSet.has(r.uid))
    selectedQuoteRowIds.value = []
    quoteBatchDeleteMode.value = false
  })
}

function onBatchDeleteClick() {
  if (!quoteBatchDeleteMode.value) {
    quoteBatchDeleteMode.value = true
    selectedQuoteRowIds.value = []
    return
  }
  if (!selectedQuoteRowIds.value.length) {
    window.alert('请先勾选要删除的报价明细')
    return
  }
  requestBatchRemoveQuoteItems()
}

function exitQuoteBatchDeleteMode() {
  quoteBatchDeleteMode.value = false
  selectedQuoteRowIds.value = []
}

function openAddCategoryDialog() {
  newCategoryName.value = ''
  addCategoryVisible.value = true
}

function submitAddCategory() {
  const name = String(newCategoryName.value || '').trim()
  if (!name) return window.alert('请输入大类名称')
  if (!quoteCategoryOptions.value.includes(name)) {
    customQuoteCategories.value.push(name)
  }
  quoteForm.value.category = name
  addCategoryVisible.value = false
}

function requestRemoveExtraItem(row, item) {
  const name = item?.name || '该非标项目'
  openInlineDeleteConfirm(`确认删除非标项目「${name}」吗？`, async () => {
    if (item?.type === 'rule') await removeRuleFromRow(row, item.id)
    else await removeCustomRuleFromRow(row, item.index)
  })
}

function ruleModeText(mode) {
  if (mode === 'PERCENT') return '%'
  if (mode === 'FIXED_PER_ITEM') return '元/件'
  return '元/平'
}

function nextStatuses(current) {
  return ORDER_FLOW[current] || []
}

function orderStatusClass(status) {
  const map = {
    报价中: 'order-quote',
    待客户确认: 'order-confirm',
    待财务确认: 'order-pay',
    财务已收款: 'order-paid',
    待排产: 'order-scheduled',
    生产中: 'order-producing',
    已完成: 'order-finished',
    待发货: 'order-ship-wait',
    已发货: 'order-shipped',
  }
  return map[status] || 'status-draft'
}

function showProgressText(row) {
  const status = row?.status
  if (status === '生产中' || status === '已完成') {
    const n = Number(row?.productionProgress ?? 0)
    return `${Math.max(0, Math.min(100, n))}%`
  }
  return '-'
}

function getOrderOwner(row) {
  const raw = row?.customFields
  if (!raw) return '-'
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return parsed?.负责人 || parsed?.owner || '-'
  } catch {
    return '-'
  }
}

function getOrderMasterNo(row) {
  const raw = row?.customFields
  if (!raw) return '-'
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return parsed?.总合同号 || parsed?.masterContractNo || '-'
  } catch {
    return '-'
  }
}

function getFactoryOrderNo(row) {
  return row?.factoryOrderNo || row?.contractNo || '-'
}

function getCustomerOrderNo(row) {
  const direct = row?.customerOrderNo
  if (direct) return direct
  const ext = parseOrderCustomFields(row)?.子合同扩展 || {}
  return ext?.客户订单号 || '-'
}

function parseCustomerCustomFields(customer) {
  const raw = customer?.customFields || customer?.custom_fields
  if (!raw) return {}
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch {
    return {}
  }
}

function customerProvinceCity(customer) {
  const ext = parseCustomerCustomFields(customer)
  const province = ext.province || ext.省份 || ''
  const city = ext.city || ext.市区 || ''
  const district = ext.areaName || ext.district || ext.区县 || extractDistrict(customer?.address)
  const text = [province, city, district].filter(Boolean).join('')
  return text || '-'
}

function extractDistrict(address) {
  const match = String(address || '').match(/([\u4e00-\u9fa5A-Za-z0-9]+(?:区|县|市))/)
  return match?.[1] || ''
}

function customerOptionText(c) {
  return `${customerProvinceCity(c)} / ${c.customerName || '-'}`
}

function parseOrderCustomFields(row) {
  const raw = row?.customFields
  if (!raw) return {}
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch {
    return {}
  }
}

function resetOrderFormForChild() {
  orderForm.value = {
    customerId: '', projectId: '', customerOrderId: '', factoryOrderPrefix: 'V', factoryOrderNo: '',
    customerName: '', customerPhone: '', customerAddress: '', demandDesc: '', owner: '',
    customerOrderNo: '', doorType: '', doorTypeRemark: '', colorName: '', colorRemark: '',
    materialName: '', coreBoardType: '', colorSeries: '', woodVeneer: '', doorThickness: '',
    boardMaterial: '', wallPanelModel: '', wallPanelMaterial: '', receiveDate: '', quoteDate: '',
    exportDate: '', workDays: '', quoterName: '', doorTechnician: '', smallPartTechnician: ''
  }
  customerKeyword.value = ''
  customerProjects.value = []
  customerOrderOptions.value = []
  orderInlineProjectVisible.value = false
  orderInlineProjectForm.value = { projectName: '', projectAddress: '', projectManager: '', remark: '' }
  orderInlineCustomerOrderVisible.value = false
  orderInlineCustomerOrderForm.value = { customerOrderNo: '', customerOrderName: '', remark: '' }
  assignmentForm.value = { serviceStaff: loginUserName.value || '', engineer: '', remark: '' }
  selectedEngineer.value = ''
  orderAssignmentMode.value = 'ASSIGNED'
  pendingAssignments.value = []
}

function todayInputValue() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function autofillOrderBusinessInfo() {
  orderForm.value.owner = orderForm.value.owner || loginUserName.value || ''
  orderForm.value.receiveDate = orderForm.value.receiveDate || todayInputValue()
  const project = selectedOrderProject.value
  const demandParts = [
    orderForm.value.projectId === NO_SPECIFIC_PROJECT ? `项目：${NO_SPECIFIC_PROJECT_NAME}` : '',
    project?.projectName ? `项目：${project.projectName}` : '',
    project?.projectAddress ? `地址：${project.projectAddress}` : '',
    orderForm.value.customerOrderNo ? `客户订单号：${orderForm.value.customerOrderNo}` : ''
  ].filter(Boolean)
  orderForm.value.demandDesc = demandParts.join('；')
  orderForm.value.quoterName = quoteForm.value.operator || loginUserName.value || orderForm.value.quoterName || ''
}

async function loadCustomerOptions() {
  const kw = String(customerKeyword.value || '').trim()
  const { data } = await listCustomers(kw)
  customerOptions.value = data?.data || []
}

async function loadProjectCustomerOptions() {
  const kw = String(projectCustomerKeyword.value || '').trim()
  const { data } = await listCustomers(kw)
  projectCustomerOptions.value = data?.data || []
}

async function openCustomerDropdown(type) {
  if (type === 'project') {
    if (projectDialogSource.value === 'order') return
    projectCustomerDropdownOpen.value = true
    await loadProjectCustomerOptions()
    return
  }
  customerDropdownOpen.value = true
  await loadCustomerOptions()
}

function deferCloseCustomerDropdown(type) {
  window.setTimeout(() => {
    if (type === 'project') projectCustomerDropdownOpen.value = false
    else customerDropdownOpen.value = false
  }, 160)
}

async function onCustomerKeywordInput() {
  customerDropdownOpen.value = true
  await loadCustomerOptions()
}

async function onProjectCustomerKeywordInput() {
  projectCustomerDropdownOpen.value = true
  await loadProjectCustomerOptions()
}

async function selectOrderCustomer(customer) {
  if (!customer) return
  orderForm.value.customerId = String(customer.id)
  orderForm.value.customerName = customer.customerName || ''
  orderForm.value.customerPhone = customer.phone || ''
  orderForm.value.customerAddress = customer.address || ''
  customerKeyword.value = customerOptionText(customer)
  orderForm.value.projectId = ''
  orderForm.value.customerOrderId = ''
  orderForm.value.customerOrderNo = ''
  customerOrderOptions.value = []
  orderInlineCustomerOrderVisible.value = false
  orderInlineCustomerOrderForm.value = { customerOrderNo: '', customerOrderName: '', remark: '' }
  orderInlineProjectVisible.value = false
  orderInlineProjectForm.value = { projectName: '', projectAddress: '', projectManager: '', remark: '' }
  customerDropdownOpen.value = false
  autofillOrderBusinessInfo()
  await loadCustomerProjects(customer.id)
}

function selectProjectCustomer(customer) {
  if (!customer) return
  projectForm.value.customerId = String(customer.id)
  projectCustomerKeyword.value = customerOptionText(customer)
  projectCustomerDropdownOpen.value = false
}

async function loadCustomerProjects(customerId) {
  if (!customerId) {
    customerProjects.value = []
    return
  }
  const { data } = await listProjects(Number(customerId), '')
  customerProjects.value = data?.data || []
}

async function loadOrderCustomerOrders(projectId) {
  if (!projectId || projectId === NO_SPECIFIC_PROJECT) {
    customerOrderOptions.value = []
    return
  }
  const { data } = await listCustomerOrders(Number(projectId), undefined, '')
  customerOrderOptions.value = data?.data || []
}

async function onOrderProjectChange() {
  orderForm.value.customerOrderId = ''
  orderForm.value.customerOrderNo = ''
  customerOrderOptions.value = []
  orderInlineCustomerOrderVisible.value = false
  orderInlineCustomerOrderForm.value = { customerOrderNo: '', customerOrderName: '', remark: '' }
  if (orderForm.value.projectId === NO_SPECIFIC_PROJECT) {
    orderInlineProjectVisible.value = false
    orderInlineProjectForm.value = { projectName: '', projectAddress: '', projectManager: '', remark: '' }
    autofillOrderBusinessInfo()
    return
  }
  const p = selectedOrderProject.value
  if (p) {
    orderForm.value.projectId = String(p.projectId)
    await loadOrderCustomerOrders(p.projectId)
    autofillOrderBusinessInfo()
  }
}

function selectedCustomerOrder() {
  return customerOrderOptions.value.find((o) => String(o.id) === String(orderForm.value.customerOrderId)) || null
}

function onOrderCustomerOrderNoInput() {
  const no = String(orderForm.value.customerOrderNo || '').trim()
  const row = customerOrderOptions.value.find((o) => String(o.customerOrderNo || '').trim() === no)
  orderForm.value.customerOrderId = row?.id ? String(row.id) : ''
  autofillOrderBusinessInfo()
}

function showInlineCustomerOrderCreate() {
  if (!selectedOrderProject.value) return window.alert('请先选择项目')
  orderInlineCustomerOrderVisible.value = true
}

function hideInlineCustomerOrderCreate() {
  orderInlineCustomerOrderVisible.value = false
}

async function saveInlineCustomerOrder() {
  const project = selectedOrderProject.value
  if (!project) return window.alert('请先选择项目')
  const customerOrderNo = String(orderInlineCustomerOrderForm.value.customerOrderNo || '').trim()
  if (!customerOrderNo) return window.alert('请填写客户订单号')
  const { data } = await createCustomerOrder({
    customerId: Number(orderForm.value.customerId),
    projectId: Number(project.projectId),
    customerOrderNo,
    customerOrderName: orderInlineCustomerOrderForm.value.customerOrderName,
    remark: orderInlineCustomerOrderForm.value.remark
  })
  if (data?.code !== 0) return window.alert(data?.message || '创建客户订单号失败')
  const row = data.data
  await loadOrderCustomerOrders(project.projectId)
  orderForm.value.customerOrderId = String(row.id)
  orderForm.value.customerOrderNo = row.customerOrderNo || customerOrderNo
  orderInlineCustomerOrderVisible.value = false
  orderInlineCustomerOrderForm.value = { customerOrderNo: '', customerOrderName: '', remark: '' }
  autofillOrderBusinessInfo()
  window.alert(`客户订单号创建成功：${row.customerOrderNo || customerOrderNo}`)
}

function showInlineProjectCreate() {
  if (!orderForm.value.customerId) return window.alert('请先选择客户')
  orderInlineProjectVisible.value = true
  if (!orderInlineProjectForm.value.projectManager) {
    orderInlineProjectForm.value.projectManager = orderForm.value.owner || loginUserName.value || ''
  }
}

function hideInlineProjectCreate() {
  orderInlineProjectVisible.value = false
}

async function saveInlineProject() {
  if (!orderForm.value.customerId) return window.alert('请先选择客户')
  const projectName = String(orderInlineProjectForm.value.projectName || '').trim()
  if (!projectName) return window.alert('请填写项目名称；如果确实没有具体项目，请直接选择“无具体项目”')
  const { data } = await createProject({
    customerId: Number(orderForm.value.customerId),
    projectName,
    projectAddress: orderInlineProjectForm.value.projectAddress,
    projectManager: orderInlineProjectForm.value.projectManager,
    remark: orderInlineProjectForm.value.remark
  })
  if (data?.code !== 0) return window.alert(data?.message || '创建项目失败')
  const p = data.data
  await loadCustomerProjects(orderForm.value.customerId)
  orderForm.value.projectId = String(p.projectId)
  await loadOrderCustomerOrders(p.projectId)
  orderInlineProjectVisible.value = false
  orderInlineProjectForm.value = { projectName: '', projectAddress: '', projectManager: '', remark: '' }
  autofillOrderBusinessInfo()
  window.alert(`项目创建成功：${p.projectNo}`)
}

async function openFactoryOrderForm() {
  orderCreateStep.value = 2
  autofillOrderBusinessInfo()
  assignmentForm.value.serviceStaff = assignmentForm.value.serviceStaff || loginUserName.value || ''
  if (!customerOptions.value.length) await loadCustomerOptions()
}

async function openProjectDialog(source = '') {
  projectDialogSource.value = source
  projectForm.value = { customerId: '', projectName: '', projectAddress: '', projectManager: '', remark: '' }
  projectCustomerKeyword.value = ''
  if (source === 'order' && orderForm.value.customerId) {
    projectForm.value.customerId = String(orderForm.value.customerId)
    projectCustomerKeyword.value = customerKeyword.value || orderForm.value.customerName || ''
  } else {
    await loadProjectCustomerOptions()
  }
  projectDialogVisible.value = true
}

async function saveProject() {
  if (!projectForm.value.customerId) return window.alert('请先选择客户')
  const { data } = await createProject({
    customerId: Number(projectForm.value.customerId),
    projectName: projectForm.value.projectName,
    projectAddress: projectForm.value.projectAddress,
    projectManager: projectForm.value.projectManager,
    remark: projectForm.value.remark
  })
  if (data?.code !== 0) return window.alert(data?.message || '创建项目失败')
  projectDialogVisible.value = false
  const p = data.data
  if (projectDialogSource.value === 'order') {
    await loadCustomerProjects(orderForm.value.customerId)
    orderForm.value.projectId = String(p.projectId)
    await loadOrderCustomerOrders(p.projectId)
    autofillOrderBusinessInfo()
  }
  window.alert(`项目创建成功：${p.projectNo}`)
}

async function openProjectDetail(row) {
  const projectId = row?.projectId
  if (!projectId) return
  const { data } = await getProject(projectId)
  if (data?.code !== 0) return window.alert(data?.message || '项目不存在')
  projectDetail.value = data.data || {}
  projectDetailTab.value = 'overview'
  projectDetailVisible.value = true
}

function goQuoteForOrder(row) {
  quoteForm.value.contractId = String(row.id)
  activeModule.value = C.quote
  activeSubModule.value = '报价明细'
  onContractChange()
}

function pickLatestOrderByMasterNo(masterNo) {
  const target = String(masterNo || '').trim()
  if (!target) return null
  const candidates = (orderRows.value || [])
    .filter((r) => String(getOrderMasterNo(r) || '').trim() === target)
    .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
  return candidates[0] || null
}

function autofillOrderFormByMasterNo(masterNo) {
  const row = pickLatestOrderByMasterNo(masterNo)
  if (!row) return false
  const custom = parseOrderCustomFields(row)
  const ext = custom?.子合同扩展 || {}
  orderForm.value.customerName = row.customerName || ''
  orderForm.value.customerPhone = row.customerPhone || ''
  orderForm.value.customerAddress = row.customerAddress || ''
  orderForm.value.demandDesc = row.demandDesc || ''
  orderForm.value.owner = custom?.负责人 || custom?.owner || ''
  orderForm.value.customerOrderNo = ext?.客户订单号 || ''
  orderForm.value.doorType = ext?.门型 || ''
  orderForm.value.doorTypeRemark = ext?.门型备注 || ''
  orderForm.value.colorName = ext?.颜色 || ''
  orderForm.value.colorRemark = ext?.颜色备注 || ''
  orderForm.value.materialName = ext?.材质 || ''
  orderForm.value.coreBoardType = ext?.芯板类型 || ''
  orderForm.value.colorSeries = ext?.色系 || ''
  orderForm.value.woodVeneer = ext?.木皮 || ''
  orderForm.value.doorThickness = ext?.门板厚度 || ''
  orderForm.value.boardMaterial = ext?.板材材质 || ''
  orderForm.value.wallPanelModel = ext?.墙板型号 || ''
  orderForm.value.wallPanelMaterial = ext?.墙板材质 || ''
  orderForm.value.receiveDate = ext?.接单日期 || ''
  orderForm.value.quoteDate = ext?.报价日期 || ''
  orderForm.value.exportDate = ext?.出口日期 || ''
  orderForm.value.workDays = ext?.工期 || ''
  orderForm.value.quoterName = ext?.报价员 || ''
  orderForm.value.doorTechnician = ext?.门板工艺员 || ''
  orderForm.value.smallPartTechnician = ext?.小件工艺员 || ''
  return true
}

function goOrderCreateStep2() {
  const no = String(masterContractForm.value.masterContractNo || '').trim()
  if (!no) return window.alert('请先输入总合同号')
  masterContractForm.value.masterContractNo = no
  autofillOrderFormByMasterNo(no)
  orderCreateStep.value = 2
}

function backToOrderCreateStep1() {
  orderCreateStep.value = 1
}

function switchToNewMasterContract() {
  orderCreateStep.value = 1
  masterContractForm.value.masterContractNo = ''
  resetOrderFormForChild()
}

async function openOrderEdit(row) {
  const custom = parseOrderCustomFields(row)
  orderEditId.value = row.id
  orderEditForm.value = {
    customerId: row.customerId || '',
    projectId: row.projectId || '',
    customerOrderId: row.customerOrderId || '',
    factoryOrderNo: getFactoryOrderNo(row) === '-' ? '' : getFactoryOrderNo(row),
    customerOrderNo: getCustomerOrderNo(row) === '-' ? '' : getCustomerOrderNo(row),
    customerName: row.customerName || '',
    customerPhone: row.customerPhone || '',
    customerAddress: row.customerAddress || '',
    demandDesc: row.demandDesc || '',
    customFields: JSON.stringify(custom || {}),
    assignmentId: '',
    assignmentServiceStaff: '',
    assignmentEngineer: '',
    assignmentRemark: ''
  }
  try {
    const { data } = await listQuoteAssignments(Number(row.id), '')
    const first = (data?.data || [])[0]
    if (first) {
      orderEditForm.value.assignmentId = first.id ? String(first.id) : ''
      orderEditForm.value.assignmentServiceStaff = first.service_staff || ''
      orderEditForm.value.assignmentEngineer = first.engineer || ''
      orderEditForm.value.assignmentRemark = first.remark || ''
    }
  } catch {
  }
  orderEditVisible.value = true
}

function openOrderDetailEdit(row) {
  const custom = parseOrderCustomFields(row)
  const ext = custom?.子合同扩展 || {}
  orderDetailEditId.value = row.id
  orderDetailEditForm.value = {
    masterContractNo: custom?.总合同号 || custom?.masterContractNo || '',
    customerName: row.customerName || '',
    customerPhone: row.customerPhone || '',
    customerAddress: row.customerAddress || '',
    customerOrderNo: row.customerOrderNo || ext?.客户订单号 || '',
    owner: custom?.负责人 || custom?.owner || '',
    demandDesc: row.demandDesc || '',
    doorType: ext?.门型 || '',
    colorName: ext?.颜色 || '',
    materialName: ext?.材质 || '',
    colorSeries: ext?.色系 || '',
    doorThickness: ext?.门板厚度 || '',
    doorTypeRemark: ext?.门型备注 || '',
    colorRemark: ext?.颜色备注 || '',
    coreBoardType: ext?.芯板类型 || '',
    woodVeneer: ext?.木皮 || '',
    boardMaterial: ext?.板材材质 || '',
    wallPanelModel: ext?.墙板型号 || '',
    wallPanelMaterial: ext?.墙板材质 || '',
    quoteDate: ext?.报价日期 || '',
    receiveDate: ext?.接单日期 || '',
    exportDate: ext?.出口日期 || '',
    quoterName: ext?.报价员 || '',
    workDays: ext?.工期 || '',
    doorTechnician: ext?.门板工艺员 || '',
    smallPartTechnician: ext?.小件工艺员 || ''
  }
  orderDetailEditVisible.value = true
}

async function saveOrderEdit() {
  if (!orderEditId.value) return
  if (!orderEditForm.value.customerName || !orderEditForm.value.customerPhone) {
    return window.alert('请填写客户姓名、电话')
  }
  const payload = {
    customerId: orderEditForm.value.customerId ? Number(orderEditForm.value.customerId) : null,
    projectId: orderEditForm.value.projectId ? Number(orderEditForm.value.projectId) : null,
    customerOrderId: null,
    factoryOrderNo: orderEditForm.value.factoryOrderNo,
    customerOrderNo: orderEditForm.value.customerOrderNo,
    customerName: orderEditForm.value.customerName,
    customerPhone: orderEditForm.value.customerPhone,
    customerAddress: orderEditForm.value.customerAddress,
    demandDesc: orderEditForm.value.demandDesc,
    customFields: orderEditForm.value.customFields || null
  }
  const { data } = await updateContract(orderEditId.value, payload)
  if (data?.code !== 0) return window.alert(data?.message || '保存失败')
  const engineer = String(orderEditForm.value.assignmentEngineer || '').trim()
  if (engineer) {
    const assignmentPayload = {
      contractId: Number(orderEditId.value),
      serviceStaff: orderEditForm.value.assignmentServiceStaff || loginUserName.value || '',
      engineer,
      status: '已分配',
      remark: orderEditForm.value.assignmentRemark || ''
    }
    if (orderEditForm.value.assignmentId) {
      const ret = await updateQuoteAssignment(Number(orderEditForm.value.assignmentId), assignmentPayload)
      if (ret?.data?.code !== 0) return window.alert(ret?.data?.message || '保存深化工程师失败')
    } else {
      const ret = await createQuoteAssignment(assignmentPayload)
      if (ret?.data?.code !== 0) return window.alert(ret?.data?.message || '保存深化工程师失败')
    }
  }
  orderEditVisible.value = false
  await loadOrders()
}

async function saveOrderDetailEdit() {
  if (!orderDetailEditId.value) return
  if (!orderDetailEditForm.value.customerName || !orderDetailEditForm.value.customerPhone) {
    return window.alert('请填写客户姓名、电话')
  }
  const custom = {
    总合同号: orderDetailEditForm.value.masterContractNo || '',
    负责人: orderDetailEditForm.value.owner || '',
    子合同扩展: {
      客户订单号: orderDetailEditForm.value.customerOrderNo || '',
      门型: orderDetailEditForm.value.doorType || '',
      门型备注: orderDetailEditForm.value.doorTypeRemark || '',
      颜色: orderDetailEditForm.value.colorName || '',
      颜色备注: orderDetailEditForm.value.colorRemark || '',
      材质: orderDetailEditForm.value.materialName || '',
      芯板类型: orderDetailEditForm.value.coreBoardType || '',
      色系: orderDetailEditForm.value.colorSeries || '',
      木皮: orderDetailEditForm.value.woodVeneer || '',
      门板厚度: orderDetailEditForm.value.doorThickness || '',
      板材材质: orderDetailEditForm.value.boardMaterial || '',
      墙板型号: orderDetailEditForm.value.wallPanelModel || '',
      墙板材质: orderDetailEditForm.value.wallPanelMaterial || '',
      接单日期: orderDetailEditForm.value.receiveDate || '',
      报价日期: orderDetailEditForm.value.quoteDate || '',
      出口日期: orderDetailEditForm.value.exportDate || '',
      工期: orderDetailEditForm.value.workDays || '',
      报价员: orderDetailEditForm.value.quoterName || '',
      门板工艺员: orderDetailEditForm.value.doorTechnician || '',
      小件工艺员: orderDetailEditForm.value.smallPartTechnician || ''
    }
  }
  const payload = {
    customerId: null,
    projectId: null,
    factoryOrderNo: '',
    customerOrderNo: orderDetailEditForm.value.customerOrderNo || '',
    customerName: orderDetailEditForm.value.customerName,
    customerPhone: orderDetailEditForm.value.customerPhone,
    customerAddress: orderDetailEditForm.value.customerAddress,
    demandDesc: orderDetailEditForm.value.demandDesc,
    customFields: JSON.stringify(custom)
  }
  const { data } = await updateContract(orderDetailEditId.value, payload)
  if (data?.code !== 0) return window.alert(data?.message || '保存失败')
  orderDetailEditVisible.value = false
  await loadOrders()
}

function requestDeleteOrder(row) {
  const name = row?.contractNo || `ID:${row?.id || ''}`
  openInlineDeleteConfirm(`确认删除合同「${name}」吗？删除后不可恢复。`, async () => {
    const { data } = await deleteContract(row.id)
    if (data?.code !== 0) return window.alert(data?.message || '删除失败')
    await loadOrders()
  })
}

async function loadOrderQuoteProgress(contractId) {
  const key = String(contractId)
  try {
    const { data } = await listQuoteAssignments(Number(contractId), '')
    const rows = data?.data || []
    const total = rows.length
    const done = rows.filter((x) => Number(x.quote_count || 0) > 0).length
    const pending = rows.filter((x) => Number(x.quote_count || 0) <= 0).map((x) => x.engineer || `分配ID:${x.id}`)
    orderQuoteProgressMap.value[key] = { done, total, pending }
  } catch {
    orderQuoteProgressMap.value[key] = { done: 0, total: 0, pending: [] }
  }
}

function orderQuoteProgressText(row) {
  const p = orderQuoteProgressMap.value[String(row.id)] || { done: 0, total: 0 }
  if (!Number(p.total || 0)) return '-/-'
  return `${p.done}/${p.total}`
}

function orderQuotePendingTooltip(row) {
  const p = orderQuoteProgressMap.value[String(row.id)] || { pending: [], total: 0 }
  if (!p.total) return '暂无分配工程师'
  return p.pending?.length ? `未完成：${p.pending.join('、')}` : '已全部完成报价'
}

function orderProgressText(row) {
  const status = String(row?.status || '')
  if (['报价中', '待客户确认'].includes(status)) return orderQuoteProgressText(row)
  if (['待排产', '生产中'].includes(status)) {
    const n = Math.max(0, Math.min(100, Number(row?.productionProgress ?? 0)))
    return `${n}/100`
  }
  return '-/-'
}

function orderProgressTooltip(row) {
  const status = String(row?.status || '')
  if (['报价中', '待客户确认'].includes(status)) return orderQuotePendingTooltip(row)
  if (['待排产', '生产中'].includes(status)) {
    const n = Math.max(0, Math.min(100, Number(row?.productionProgress ?? 0)))
    return `生产进度：${n}%`
  }
  return '当前状态无需显示进度'
}

async function loadOrders() {
  const { data } = await listContracts()
  orderRows.value = (data?.data || []).map((r) => ({ ...r, _nextStatus: '', _progress: r.productionProgress ?? 0 }))
  const financeContracts = orderRows.value
    .filter((r) => ['待财务确认', '财务已收款', '待排产'].includes(String(r.status || '')))
    .map((r) => r.id)
  await Promise.all([
    ...financeContracts.map((id) => loadFinanceVouchers(id)),
    ...orderRows.value.map((r) => loadOrderQuoteProgress(r.id))
  ])
}

async function ensureOrderProjectId() {
  if (!orderForm.value.customerId) return ''
  if (orderForm.value.projectId && orderForm.value.projectId !== NO_SPECIFIC_PROJECT) {
    return orderForm.value.projectId
  }
  if (orderForm.value.projectId !== NO_SPECIFIC_PROJECT) return ''

  let project = customerProjects.value.find((p) => String(p.projectName || '').trim() === NO_SPECIFIC_PROJECT_NAME)
  if (!project) {
    const { data } = await createProject({
      customerId: Number(orderForm.value.customerId),
      projectName: NO_SPECIFIC_PROJECT_NAME,
      projectAddress: orderForm.value.customerAddress || '',
      projectManager: orderForm.value.owner || loginUserName.value || '',
      remark: '系统为无具体项目订单自动创建'
    })
    if (data?.code !== 0) throw new Error(data?.message || '创建无具体项目失败')
    project = data.data
    await loadCustomerProjects(orderForm.value.customerId)
  }
  orderForm.value.projectId = String(project.projectId)
  autofillOrderBusinessInfo()
  return orderForm.value.projectId
}

async function createOrder(enterQuote = false) {
  if (!orderForm.value.customerId) return window.alert('请先选择客户')
  if (orderInlineProjectVisible.value) return window.alert('请先保存或取消旁边填写的项目')
  let ensuredProjectId = ''
  try {
    ensuredProjectId = await ensureOrderProjectId()
  } catch (e) {
    return window.alert(e?.message || '项目处理失败')
  }
  if (!ensuredProjectId) return window.alert('请先选择项目，或选择“无具体项目”')
  onOrderCustomerOrderNoInput()
  if (!String(orderForm.value.customerOrderNo || '').trim()) return window.alert('请填写客户订单号')
  autofillOrderBusinessInfo()
  const customFields = {
    总合同号: selectedOrderProject.value?.projectNo || '',
    客户项目号: selectedOrderProject.value?.projectNo || '',
    项目名称: selectedOrderProject.value?.projectName || '',
    负责人: orderForm.value.owner || '',
    子合同扩展: {
      客户订单号: orderForm.value.customerOrderNo || '',
      门型: orderForm.value.doorType || '',
      门型备注: orderForm.value.doorTypeRemark || '',
      颜色: orderForm.value.colorName || '',
      颜色备注: orderForm.value.colorRemark || '',
      材质: orderForm.value.materialName || '',
      芯板类型: orderForm.value.coreBoardType || '',
      色系: orderForm.value.colorSeries || '',
      木皮: orderForm.value.woodVeneer || '',
      门板厚度: orderForm.value.doorThickness || '',
      板材材质: orderForm.value.boardMaterial || '',
      墙板型号: orderForm.value.wallPanelModel || '',
      墙板材质: orderForm.value.wallPanelMaterial || '',
      接单日期: orderForm.value.receiveDate || '',
      报价日期: orderForm.value.quoteDate || '',
      出口日期: orderForm.value.exportDate || '',
      工期: orderForm.value.workDays || '',
      报价员: orderForm.value.quoterName || quoteForm.value.operator || '',
      门板工艺员: orderForm.value.doorTechnician || '',
      小件工艺员: orderForm.value.smallPartTechnician || ''
    }
  }
  const payload = {
    customerId: Number(orderForm.value.customerId),
    projectId: Number(ensuredProjectId),
    customerOrderId: orderForm.value.customerOrderId ? Number(orderForm.value.customerOrderId) : null,
    factoryOrderPrefix: orderForm.value.factoryOrderPrefix || 'V',
    factoryOrderNo: '',
    customerOrderNo: orderForm.value.customerOrderNo,
    customerName: orderForm.value.customerName,
    customerPhone: orderForm.value.customerPhone,
    customerAddress: orderForm.value.customerAddress,
    demandDesc: orderForm.value.demandDesc,
    customFields: JSON.stringify(customFields)
  }
  const { data } = await createContract(payload)
  if (data.code !== 0) return window.alert(data.message)
  const isPoolOrder = orderAssignmentMode.value === 'POOL'
  try {
    await createAssignmentsForOrder(data.data)
  } catch (e) {
    window.alert(e?.message || '工厂订单已创建，但报价分配创建失败')
  }
  resetOrderFormForChild()
  await loadOrders()
  if (enterQuote && !isPoolOrder) {
    goQuoteForOrder(data.data)
  } else {
    const suffix = isPoolOrder ? '，已进入深化订单池' : ''
    window.alert(`创建成功，工厂订单号：${data.data.factoryOrderNo || data.data.contractNo}${suffix}`)
  }
}

async function createAssignmentsForOrder(orderRow) {
  const contractId = Number(orderRow?.id || 0)
  if (!contractId) return
  const isPool = orderAssignmentMode.value === 'POOL'
  const engineer = String(selectedEngineer.value || '').trim()
  if (!isPool && !engineer) throw new Error('请选择深化工程师，或选择放入订单池')
  const { data } = await createQuoteAssignment({
    contractId,
    serviceStaff: assignmentForm.value.serviceStaff || loginUserName.value || '',
    engineer: isPool ? '' : engineer,
    status: isPool ? '待领取' : '已分配',
    remark: assignmentForm.value.remark || ''
  })
  if (data?.code !== 0) throw new Error(data?.message || '创建报价分配失败')
}

async function advanceOrderStatus(row) {
  if (!row._nextStatus) return window.alert('请先选择下一状态')
  const payload = { targetStatus: row._nextStatus, operator: '系统管理员', remark: `订单状态更新：${row._nextStatus}` }
  if (row._nextStatus === '生产中') payload.productionProgress = Number(row._progress || 0)
  const { data } = await updateContractStatus(row.id, payload)
  if (data.code !== 0) return window.alert(data.message)
  await loadOrders()
}

async function openOrderLogs(row) {
  try {
    const contractId = row?.id || row?.contract_id
    if (!contractId) return window.alert('缺少合同ID，无法读取日志')
    const { data } = await listContractLogs(contractId)
    if (data?.code !== 0) return window.alert(data?.message || '读取订单状态日志失败')
    currentOrderNo.value = row?.contractNo || row?.contract_no || row?.contractNoText || '-'
    orderLogs.value = Array.isArray(data?.data) ? data.data : []
    orderLogsVisible.value = true
  } catch (e) {
    window.alert(`读取订单状态日志失败：${e?.message || '网络异常'}`)
  }
}

function getProductCategory(p) {
  if (!p) return ''
  const productType = String(p.type || '').trim()
  if (productType) return productType
  if (p.category) return String(p.category).trim()
  try {
    const cf = p.custom_fields ? (typeof p.custom_fields === 'string' ? JSON.parse(p.custom_fields) : p.custom_fields) : {}
    const customCategory = String(cf?.category || cf?.大类 || '').trim()
    if (customCategory) return customCategory
  } catch {
  }
  return ''
}

function uniqueText(values) {
  return Array.from(new Set(values.map((v) => String(v || '').trim()).filter(Boolean)))
}

function defaultQuoteCategory() {
  return quoteCategoryOptions.value[0] || ''
}

function ensureQuoteCategory() {
  if (!quoteForm.value.category) {
    quoteForm.value.category = defaultQuoteCategory()
  }
}

function rowOptionListId(row, name) {
  return `quote_${name}_${row.uid}`
}

function catalogForCategory(category) {
  const c = String(category || '').trim()
  if (!c) return productCatalog.value
  return productCatalog.value.filter((p) => getProductCategory(p) === c)
}

function rowCatalog(row) {
  return catalogForCategory(row?.category || quoteForm.value.category)
}

function rowProductStyleOptions(row) {
  return uniqueText(rowCatalog(row).map((p) => p.product_name))
}

function catalogForRowStyle(row) {
  const style = String(row?.productType || '').trim()
  const list = rowCatalog(row)
  if (!style) return list
  return list.filter((p) => String(p.product_name || '').trim() === style)
}

function rowModelOptions(row) {
  return uniqueText(catalogForRowStyle(row).map((p) => p.model))
}

function rowMaterialOptions(row) {
  return uniqueText(catalogForRowStyle(row).map((p) => p.material_name))
}

function rowColorOptions(row) {
  return uniqueText(catalogForRowStyle(row).map((p) => p.color))
}

function catalogForHandleColor(row) {
  const model = String(row?.modelKeyword || '').trim()
  const list = catalogForRowStyle(row)
  if (!model) return list
  return list.filter((p) => String(p.model || '').trim() === model)
}

function rowHandleColorOptions(row) {
  return uniqueText(catalogForHandleColor(row).map((p) => p.handle_color))
}

function syncSingleHandleColor(row) {
  const options = rowHandleColorOptions(row)
  row.handleColor = options.length === 1 ? options[0] : ''
}

function categoryFromSavedDetails(details) {
  for (const d of details || []) {
    const productName = String(d.product_name || '').trim()
    const material = String(d.material_structure || '').trim()
    const product = productCatalog.value.find((p) => {
      if (productName && String(p.model || '').trim() !== productName) return false
      if (material && String(p.material_name || '').trim() !== material) return false
      return true
    }) || productCatalog.value.find((p) => productName && String(p.model || '').trim() === productName)
      || productCatalog.value.find((p) => productName && String(p.product_name || '').trim() === productName)
    const category = getProductCategory(product)
    if (category) return category
  }
  return quoteForm.value.category || defaultQuoteCategory()
}

function onQuoteCategoryChange() {
  ensureQuoteCategory()
  addQuoteItem()
}

function onQuoteCategoryTabClick(category) {
  quoteForm.value.category = String(category || '').trim()
  addQuoteItem({ category: quoteForm.value.category })
}

function syncRowCategory(row) {
  row.productType = ''
  row.modelKeyword = ''
  row.productCode = ''
  row.productModel = ''
  row.productName = ''
  row.materialStructure = ''
  row.color = ''
  row.handleColor = ''
  row.thickness = ''
  row.baseUnitPrice = 0
  row.areaM2 = 0
  row.amount = 0
  row.specialAdjustTotal = 0
  row.finalUnitPrice = 0
  row.selectedRuleIds = []
  row.ruleOptions = []
}

function syncRowProductStyle(row) {
  row.modelKeyword = ''
  row.productCode = ''
  row.productModel = ''
  row.productName = ''
  row.materialStructure = ''
  row.color = ''
  row.handleColor = ''
  row.thickness = ''
  row.baseUnitPrice = 0
  row.areaM2 = 0
  row.amount = 0
  row.specialAdjustTotal = 0
  row.finalUnitPrice = 0
  row.selectedRuleIds = []
  row.ruleOptions = []
  syncSingleHandleColor(row)
}

function onNonStandardChange(row) {
  if (row.nonStandard) {
    searchRules(row)
    return
  }
  const hasItems = (row.selectedRuleIds || []).length || (row.customRules || []).length
  if (hasItems && !window.confirm('关闭非标将清空当前加价项目，是否继续？')) {
    row.nonStandard = true
    return
  }
  row.ruleKeyword = ''
  row.ruleOptions = []
  row.selectedRuleIds = []
  row.customRules = []
  row.customRule = { ruleName: '', adjustMode: 'FIXED_PER_M2', adjustValue: 0, unitDesc: '元/平', minAreaM2: null, ruleQuantity: null, minCharge: null, maxCharge: null, remark: '' }
  recalcQuoteRow(row)
}

function catalogForRow(row) {
  return catalogForRowStyle(row)
}

async function loadPriceRules() {
  const category = activeSubModule.value === C.priceRule ? '' : processRuleCategory.value
  const { data } = await listQuoteRules(priceRuleKeyword.value || '', category || '')
  priceRuleRows.value = data?.data || []
}

async function savePriceRule() {
  if (!newPriceRule.value.ruleName) return window.alert('请填写规则名称')
  const payload = {
    ruleCategory: String(newPriceRule.value.ruleCategory || '').trim(),
    ruleName: newPriceRule.value.ruleName,
    adjustMode: newPriceRule.value.adjustMode,
    adjustValue: Number(newPriceRule.value.adjustValue || 0),
    unitDesc: newPriceRule.value.unitDesc || '',
    minAreaM2: newPriceRule.value.minAreaM2 === null || newPriceRule.value.minAreaM2 === '' ? null : Number(newPriceRule.value.minAreaM2),
    minCharge: newPriceRule.value.minCharge === null || newPriceRule.value.minCharge === '' ? null : Number(newPriceRule.value.minCharge),
    maxCharge: newPriceRule.value.maxCharge === null || newPriceRule.value.maxCharge === '' ? null : Number(newPriceRule.value.maxCharge),
    enabled: ruleStatusValue(newPriceRule.value.status),
    remark: newPriceRule.value.remark || ''
  }
  const { data } = await createQuoteRule(payload)
  if (data.code !== 0) return window.alert(data.message || '新增失败')
  newPriceRule.value = { ruleCategory: newPriceRule.value.ruleCategory || defaultQuoteCategory(), ruleName: '', adjustMode: 'FIXED_PER_M2', adjustValue: 0, unitDesc: '元/平', minAreaM2: null, minCharge: null, maxCharge: null, status: '启用', remark: '' }
  await loadPriceRules()
  window.alert('规则新增成功')
}

function rulePayloadFromForm(form) {
  return {
    ruleCategory: String(form.ruleCategory || '').trim(),
    ruleName: form.ruleName,
    adjustMode: form.adjustMode,
    adjustValue: Number(form.adjustValue || 0),
    unitDesc: form.unitDesc || '',
    minAreaM2: form.minAreaM2 === null || form.minAreaM2 === '' ? null : Number(form.minAreaM2),
    minCharge: form.minCharge === null || form.minCharge === '' ? null : Number(form.minCharge),
    maxCharge: form.maxCharge === null || form.maxCharge === '' ? null : Number(form.maxCharge),
    enabled: ruleStatusValue(form.status),
    remark: form.remark || ''
  }
}

function openRuleEdit(row) {
  ruleEditId.value = row.id
  ruleEditForm.value = {
    ruleCategory: row.rule_category || '',
    ruleName: row.rule_name || '',
    adjustMode: row.adjust_mode || 'FIXED_PER_M2',
    adjustValue: Number(row.adjust_value || 0),
    unitDesc: row.unit_desc || '',
    minAreaM2: row.min_area_m2 ?? null,
    minCharge: row.min_charge ?? null,
    maxCharge: row.max_charge ?? null,
    status: ruleStatusText(row.enabled),
    remark: row.remark || ''
  }
  ruleEditVisible.value = true
}

function viewRuleDetail(row) {
  ruleDetailData.value = { ...row }
  ruleDetailVisible.value = true
}

async function submitRuleEdit() {
  if (!ruleEditForm.value.ruleName) return window.alert('请填写规则名称')
  const { data } = await updateQuoteRule(ruleEditId.value, rulePayloadFromForm(ruleEditForm.value))
  if (data.code !== 0) return window.alert(data.message || '保存失败')
  ruleEditVisible.value = false
  await loadPriceRules()
}

async function quickRuleStatus(row, status) {
  const form = {
    ruleCategory: row.rule_category || '',
    ruleName: row.rule_name || '',
    adjustMode: row.adjust_mode || 'FIXED_PER_M2',
    adjustValue: Number(row.adjust_value || 0),
    unitDesc: row.unit_desc || '',
    minAreaM2: row.min_area_m2 ?? null,
    minCharge: row.min_charge ?? null,
    maxCharge: row.max_charge ?? null,
    status,
    remark: row.remark || ''
  }
  const { data } = await updateQuoteRule(row.id, rulePayloadFromForm(form))
  if (data.code !== 0) return window.alert(data.message || '状态更新失败')
  await loadPriceRules()
}

async function removeRule(row) {
  if (!window.confirm(`确认删除规则「${row.rule_name}」吗？`)) return
  const { data } = await deleteQuoteRule(row.id)
  if (data.code !== 0) return window.alert(data.message || '删除失败')
  await loadPriceRules()
}

async function searchRules(row) {
  const { data } = await listQuoteRules(row.ruleKeyword || '', row.category || quoteForm.value.category || '')
  row.ruleOptions = (data?.data || []).filter((r) => Number(r.enabled) === 1)
}

async function searchProductModels(row) {
  const keyword = String(row.modelKeyword || '').trim()
  const { data } = await listProducts(keyword)
  const list = (data?.data || []).filter((p) => String(p.status || '') === '启用')
  const category = String(row?.category || quoteForm.value.category || '').trim()
  const style = String(row?.productType || '').trim()
  row.modelOptions = list.filter((p) => {
    if (category && getProductCategory(p) !== category) return false
    if (style && String(p.product_name || '').trim() !== style) return false
    return String(p.model || '').includes(keyword) ||
      String(p.product_code || '').includes(keyword)
  }).slice(0, 20)
}

function pickProductModel(row, p) {
  row.productModel = p.model || ''
  row.modelKeyword = p.model || ''
  row.productCode = p.product_code || ''
  row.productName = p.model || ''
  row.productType = p.product_name || row.productType || ''
  row.materialStructure = p.material_name || ''
  row.color = p.color || ''
  row.handleColor = p.handle_color || ''
  row.thickness = p.thickness || ''
  row.thicknessUnit = p.thickness_unit || 'mm'
  row.unit = p.unit || 'm²'
  row.productStatus = p.status || ''
  row.remark = p.custom_fields || ''
  row.baseUnitPrice = Number(p.unit_price || 0)
  row.unitPriceUnit = p.unit_price_unit || '元/m²'
  row.modelOptions = []
}

async function ensureProductCatalog() {
  if (productCatalog.value.length > 0) return
  const { data } = await listProducts('')
  productCatalog.value = (data?.data || []).filter((p) => String(p.status || '') === '启用')
}

async function refreshProductCatalog() {
  const { data } = await listProducts('')
  productCatalog.value = (data?.data || []).filter((p) => String(p.status || '') === '启用')
  ensureQuoteCategory()
}

function fillRowFromProduct(row, p) {
  row.category = getProductCategory(p) || row.category || quoteForm.value.category || ''
  row.productModel = p.model || ''
  row.modelKeyword = p.model || ''
  row.productCode = p.product_code || ''
  row.productName = p.model || ''
  row.productType = p.product_name || ''
  row.materialStructure = p.material_name || ''
  row.color = p.color || ''
  row.handleColor = p.handle_color || ''
  row.thickness = p.thickness || ''
  row.thicknessUnit = p.thickness_unit || 'mm'
  row.thicknessMm = Number(p.thickness || row.thicknessMm || 0)
  row.unit = p.unit || 'm²'
  row.remark = p.custom_fields || ''
  row.baseUnitPrice = Number(p.unit_price || 0)
  row.unitPriceUnit = p.unit_price_unit || '元/m²'
}

function applyByModel(row) {
  const v = String(row.modelKeyword || '').trim()
  if (!v) return
  const matches = catalogForRow(row).filter((x) => String(x.model || '').trim() === v)
  const p = matches[0]
  if (p) {
    fillRowFromProduct(row, p)
    syncSingleHandleColor(row)
  } else {
    row.productName = v
    syncSingleHandleColor(row)
  }
}

function applyByCode(row) {
  const v = String(row.productCode || '').trim()
  if (!v) return
  const p = catalogForRow(row).find((x) => String(x.product_code || '').trim() === v)
  if (p) fillRowFromProduct(row, p)
}

function toggleRule(row, id) {
  const set = new Set(row.selectedRuleIds || [])
  if (set.has(id)) set.delete(id)
  else set.add(id)
  row.selectedRuleIds = Array.from(set)
}

async function recalcQuoteRow(row) {
  if (row.modelKeyword) {
    await calcQuoteRow(row)
    return
  }
  row.areaM2 = calcRowArea(row)
  row.specialAdjustTotal = row.nonStandard
    ? [...selectedRulesForRow(row), ...(row.customRules || [])].reduce((sum, r) => sum + estimateRuleCharge(row, r), 0)
    : 0
  row.amount = Number((Number(row.areaM2 || 0) * Number(row.baseUnitPrice || 0) + Number(row.specialAdjustTotal || 0)).toFixed(2))
  row.finalUnitPrice = Number(row.areaM2 || 0) ? Number((row.amount / Number(row.areaM2)).toFixed(2)) : 0
  row.formulaText = buildFormulaText(row)
}

async function addRuleToRow(row, rule) {
  if (!row.selectedRuleIds.includes(rule.id)) {
    row.selectedRuleIds.push(rule.id)
  }
  if (isQuantityRule(rule) && !rule.ruleQuantity) {
    rule.ruleQuantity = 1
  }
  if (!row.ruleOptions.some((r) => r.id === rule.id)) {
    row.ruleOptions.push(rule)
  }
  await recalcQuoteRow(row)
}

async function removeRuleFromRow(row, id) {
  row.selectedRuleIds = row.selectedRuleIds.filter((x) => x !== id)
  await recalcQuoteRow(row)
}

async function removeCustomRuleFromRow(row, index) {
  row.customRules.splice(index, 1)
  await recalcQuoteRow(row)
}

async function onRuleItemChanged(row) {
  await recalcQuoteRow(row)
}

function selectedRulesForRow(row) {
  const ids = new Set(row.selectedRuleIds || [])
  return (row.ruleOptions || []).filter((r) => ids.has(r.id))
}

function isQuantityRule(rule) {
  const mode = rule.adjust_mode || rule.adjustMode
  const unit = String(rule.unit_desc || rule.unitDesc || '')
  return mode === 'FIXED_PER_ITEM' || unit.includes('个') || unit.includes('块') || unit.includes('件') || unit.includes('把')
}

function estimateRuleCharge(row, rule) {
  const area = Number(row.areaM2 || calcRowArea(row) || 0)
  const qty = Number(row.quantity || 1)
  const base = Number(row.baseUnitPrice || 0)
  const value = Number(rule.adjust_value ?? rule.adjustValue ?? 0)
  const mode = rule.adjust_mode || rule.adjustMode || 'FIXED_PER_M2'
  const minArea = Number(rule.min_area_m2 ?? rule.minAreaM2 ?? 0)
  let calcArea = area
  if (minArea > 0 && calcArea < minArea) calcArea = minArea
  let charge = 0
  if (mode === 'PERCENT') charge = base * calcArea * value / 100
  else if (mode === 'FIXED_PER_ITEM') charge = value * Number(rule.ruleQuantity || rule.rule_quantity || qty)
  else charge = value * calcArea
  const minCharge = rule.min_charge ?? rule.minCharge
  const maxCharge = rule.max_charge ?? rule.maxCharge
  if (minCharge !== null && minCharge !== undefined && minCharge !== '' && charge < Number(minCharge)) charge = Number(minCharge)
  if (maxCharge !== null && maxCharge !== undefined && maxCharge !== '' && charge > Number(maxCharge)) charge = Number(maxCharge)
  return Number(charge || 0)
}

function calcRowArea(row) {
  return Number(((Number(row.widthMm || 0) * Number(row.heightMm || 0) / 1000000) * Number(row.quantity || 1)).toFixed(4))
}

function buildFormulaText(row) {
  const area = Number(row.areaM2 || calcRowArea(row) || 0)
  const base = Number(row.baseUnitPrice || 0)
  const baseAmount = Number((area * base).toFixed(2))
  const selectedRules = selectedRulesForRow(row)
  const customRules = row.customRules || []
  const ruleTotal = row.nonStandard
    ? [...selectedRules, ...customRules].reduce((sum, r) => sum + estimateRuleCharge(row, r), 0)
    : 0
  const parts = [`平数 ${row.widthMm || 0}×${row.heightMm || 0}÷1000000×${row.quantity || 1}=${area.toFixed(4)}`, `基础 ${area.toFixed(4)}×${base}=${baseAmount.toFixed(2)}`]
  if (row.nonStandard && (selectedRules.length || customRules.length)) {
    const detail = [...selectedRules, ...customRules].map((r) => ruleFormulaFragment(row, r)).join(' + ')
    parts.push(`非标加价 ${detail || ruleTotal.toFixed(2)}`)
  }
  parts.push(`合计 ${(baseAmount + ruleTotal).toFixed(2)}`)
  return parts.join('；')
}

function ruleFormulaFragment(row, rule) {
  const name = rule.rule_name || rule.ruleName
  const mode = rule.adjust_mode || rule.adjustMode || 'FIXED_PER_M2'
  const value = Number(rule.adjust_value ?? rule.adjustValue ?? 0)
  const area = Number(row.areaM2 || calcRowArea(row) || 0)
  const base = Number(row.baseUnitPrice || 0)
  const qty = Number(rule.ruleQuantity || rule.rule_quantity || row.quantity || 1)
  const charge = estimateRuleCharge(row, rule).toFixed(2)
  if (mode === 'PERCENT') return `${name}: ${base}×${area.toFixed(4)}×${value}%=${charge}`
  if (mode === 'FIXED_PER_ITEM') return `${name}: ${value}×${qty}=${charge}`
  return `${name}: ${value}×${area.toFixed(4)}=${charge}`
}

async function addCustomRuleToRow(row) {
  const c = row.customRule || {}
  if (!c.ruleName) return window.alert('请填写自定义工艺名称')
  row.customRules.push({ ...c })
  row.customRule = { ruleName: '', adjustMode: 'FIXED_PER_M2', adjustValue: 0, unitDesc: '元/平', minAreaM2: null, ruleQuantity: null, minCharge: null, maxCharge: null, remark: '' }
  await recalcQuoteRow(row)
}

async function saveCustomRule(row) {
  const c = row.customRule || {}
  if (!c.ruleName) return window.alert('请填写自定义工艺名称')
  const payload = {
    ruleCategory: quoteForm.value.category || '',
    ruleName: c.ruleName,
    adjustMode: c.adjustMode,
    adjustValue: Number(c.adjustValue || 0),
    unitDesc: c.unitDesc || '',
    minAreaM2: c.minAreaM2 === null || c.minAreaM2 === '' ? null : Number(c.minAreaM2),
    ruleQuantity: c.ruleQuantity === null || c.ruleQuantity === '' ? null : Number(c.ruleQuantity),
    minCharge: c.minCharge === null || c.minCharge === '' ? null : Number(c.minCharge),
    maxCharge: c.maxCharge === null || c.maxCharge === '' ? null : Number(c.maxCharge),
    enabled: 1,
    remark: c.remark || ''
  }
  const { data } = await createQuoteRule(payload)
  if (data.code !== 0) return window.alert(data.message)
  await searchRules(row)
  window.alert('已加入工艺规则库')
}

function quoteRuleToCalcRule(rule) {
  return {
    sourceRuleId: rule.id || rule.sourceRuleId || null,
    ruleName: rule.rule_name || rule.ruleName || '',
    adjustMode: rule.adjust_mode || rule.adjustMode || 'FIXED_PER_M2',
    adjustValue: Number(rule.adjust_value ?? rule.adjustValue ?? 0),
    unitDesc: rule.unit_desc || rule.unitDesc || '',
    minAreaM2: rule.min_area_m2 ?? rule.minAreaM2 ?? null,
    ruleQuantity: rule.ruleQuantity ?? rule.rule_quantity ?? null,
    minCharge: rule.min_charge ?? rule.minCharge ?? null,
    maxCharge: rule.max_charge ?? rule.maxCharge ?? null,
    remark: rule.remark || ''
  }
}

function buildCalcRules(row) {
  if (!row.nonStandard) return []
  return [...selectedRulesForRow(row), ...(row.customRules || [])].map(quoteRuleToCalcRule)
}

async function calcQuoteRow(row) {
  if (!row.modelKeyword) return window.alert('请先选择产品名称')
  const payload = {
    productName: row.productName || row.modelKeyword,
    materialStructure: row.materialStructure,
    handleColor: row.handleColor,
    widthMm: Number(row.widthMm || 0),
    heightMm: Number(row.heightMm || 0),
    thicknessMm: Number(row.thicknessMm || 0),
    quantity: Number(row.quantity || 1),
    hingeHole: row.hingeHole,
    processDesc: row.processDesc || row.remark || '',
    attachmentName: row.attachmentName,
    unit: row.unit || 'm²',
    baseUnitPrice: Number(row.baseUnitPrice || 0),
    selectedRuleIds: [],
    customRules: buildCalcRules(row),
  }
  const { data } = await calculateQuoteDetail(payload)
  if (data.code !== 0) return window.alert(data.message)
  row.areaM2 = Number(data.data.areaM2 || 0)
  row.amount = Number(data.data.amount || 0)
  row.specialAdjustTotal = Number(data.data.specialAdjustTotal || 0)
  row.finalUnitPrice = Number(data.data.finalUnitPrice || 0)
  row.appliedRules = data.data.appliedRules || []
  row.formulaText = buildFormulaText(row)
}

async function calcAllQuoteItems() {
  for (const row of quoteItems.value) {
    await calcQuoteRow(row)
  }
}

async function recalcCurrentContractQuotes() {
  if (!isAdvancedUser.value) return window.alert('仅高级权限可执行整合同重算')
  const contractId = Number(quoteForm.value.contractId || 0)
  if (!contractId) return window.alert('请先选择工厂订单号')
  const { data: quoteListResp } = await listQuoteOrders('')
  const allRows = quoteListResp?.data || []
  const targetRows = allRows.filter((r) => Number(r.contract_id || 0) === contractId)
  if (!targetRows.length) return window.alert('当前合同暂无已保存报价单')
  let ok = 0
  let fail = 0
  for (const row of targetRows) {
    const quoteId = Number(row.quote_id || row.quoteId || row.id || 0)
    if (!quoteId) {
      fail++
      continue
    }
    try {
      const [mainResp, detailResp] = await Promise.all([getQuoteOrder(quoteId), listQuoteDetails(quoteId)])
      if (mainResp?.data?.code !== 0 || detailResp?.data?.code !== 0) throw new Error('读取报价失败')
      const main = mainResp.data.data || {}
      const details = detailResp.data.data || []
      const items = []
      for (const d of details) {
        const req = {
          productName: d.product_name || '',
          materialStructure: d.material_structure || '',
          handleColor: d.handle_color || '',
          widthMm: Number(d.width_mm || 0),
          heightMm: Number(d.height_mm || 0),
          thicknessMm: Number(d.thickness_mm || 0),
          quantity: Number(d.quantity || 1),
          hingeHole: d.hinge_hole || '',
          processDesc: d.process_desc || '',
          attachmentName: d.attachment_name || '',
          unit: d.unit || 'm²',
          baseUnitPrice: Number(d.base_unit_price || 0),
          selectedRuleIds: [],
          customRules: savedExtraRulesForDetail(d)
        }
        const calcResp = await calculateQuoteDetail(req)
        if (calcResp?.data?.code !== 0) throw new Error(calcResp?.data?.message || '计算失败')
        const calc = calcResp.data.data || {}
        items.push({
          ...req,
          baseUnitPrice: Number(calc.baseUnitPrice ?? req.baseUnitPrice ?? 0),
          customRules: req.customRules
        })
      }
      const payload = {
        contractId: Number(main.contract_id || contractId),
        assignmentId: Number(main.assignment_id || row.assignment_id || 0),
        serviceStaff: main.service_staff || row.service_staff || '',
        engineer: main.engineer || row.engineer || '',
        operator: main.created_by || quoteForm.value.operator || '报价员',
        quoteDesc: main.quote_desc || '',
        items
      }
      const updateResp = await updateQuoteOrder(quoteId, payload)
      if (updateResp?.data?.code !== 0) throw new Error(updateResp?.data?.message || '保存失败')
      ok++
    } catch {
      fail++
    }
  }
  await loadQuoteOrders()
  window.alert(`整合同重算完成：成功 ${ok} 条，失败 ${fail} 条`)
}

async function tryAdvanceContractAfterQuote(contractId, operator) {
  if (!contractId) return
  const { data } = await listQuoteAssignments(Number(contractId), assignmentKeyword.value || '')
  const rows = data?.data || []
  if (rows.length === 0) return
  const allDone = rows.every((a) => Number(a.quote_count || 0) > 0)
  if (!allDone) return
  try {
    const { data: resp } = await updateContractStatus(Number(contractId), {
      targetStatus: '待客户确认',
      operator: operator || '报价员',
      remark: '分配给工程师的报价均已完成，自动流转到待客户确认'
    })
    if (resp?.code === 0) {
      await loadOrders()
      await searchContracts()
    }
  } catch {
  }
}

async function doSaveQuoteOrder(options = {}) {
  const submit = options.submit !== false
  if (!quoteForm.value.contractId) return window.alert('请先填写合同ID')
  if (!quoteForm.value.assignmentId) return window.alert('请先选择深化工程师')
  if (!quoteForm.value.operator) return window.alert('请填写报价员')
  if (quoteItems.value.length === 0) return window.alert('请先添加报价明细')
  if (quoteItems.value.some((r) => !r.modelKeyword)) return window.alert('存在未选择产品名称的明细行')
  await calcAllQuoteItems()
  const payload = {
    contractId: Number(quoteForm.value.contractId),
    assignmentId: Number(quoteForm.value.assignmentId),
    submitted: submit,
    serviceStaff: selectedAssignment()?.service_staff || assignmentForm.value.serviceStaff || '',
    engineer: selectedAssignment()?.engineer || quoteForm.value.operator,
    operator: quoteForm.value.operator,
    quoteDesc: quoteForm.value.quoteDesc,
    items: quoteItems.value.map((row) => ({
      productName: row.productName || row.modelKeyword,
      materialStructure: row.materialStructure,
      handleColor: row.handleColor,
      widthMm: Number(row.widthMm || 0),
      heightMm: Number(row.heightMm || 0),
      thicknessMm: Number(row.thicknessMm || 0),
      quantity: Number(row.quantity || 1),
      hingeHole: row.hingeHole,
      processDesc: [row.productType, row.color, row.remark].filter(Boolean).join(' / '),
      attachmentName: row.attachmentName,
      unit: row.unit || 'm²',
      baseUnitPrice: Number(row.baseUnitPrice || 0),
      selectedRuleIds: [],
      customRules: buildCalcRules(row),
    })),
  }
  const { data } = quoteEditingId.value
    ? await updateQuoteOrder(Number(quoteEditingId.value), payload)
    : await saveQuoteDetails(payload)
  if (data.code !== 0) return window.alert(data.message)
  const savedId = Number(data.data?.quoteId || quoteEditingId.value || 0)
  quoteEditingId.value = null
  await loadQuoteOrders()
  if (submit) {
    await tryAdvanceContractAfterQuote(payload.contractId, quoteForm.value.operator)
  }
  await loadQuoteAssignments()
  quoteEditBaseline.value = currentQuoteEditFingerprint.value
  window.alert(`${submit ? '提交' : '暂存'}成功，报价单ID：${savedId}，总金额：${data.data.totalAmount}`)
}

function refreshQuoteSubmitCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  quoteSubmitCaptcha.value = Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

function openQuoteSubmitConfirm() {
  quoteSubmitMode.value = 'submit'
  quoteSubmitInput.value = ''
  refreshQuoteSubmitCaptcha()
  quoteSubmitConfirmVisible.value = true
}

async function confirmQuoteSubmit() {
  if (String(quoteSubmitInput.value || '').trim().toUpperCase() !== quoteSubmitCaptcha.value) {
    quoteSubmitInput.value = ''
    refreshQuoteSubmitCaptcha()
    return window.alert('验证码不正确，已刷新验证码')
  }
  await doSaveQuoteOrder({ submit: true })
  quoteSubmitConfirmVisible.value = false
}

async function saveQuoteDraft() {
  await doSaveQuoteOrder({ submit: false })
}

async function startEditQuote(row) {
  if (!confirmLeaveEditIfNeeded()) return
  const quoteId = row.quote_id || row.quoteId || row.id
  if (!quoteId) return
  const [mainResp, detailResp] = await Promise.all([getQuoteOrder(quoteId), listQuoteDetails(quoteId)])
  if (mainResp.data.code !== 0) return window.alert(mainResp.data.message || '读取报价单失败')
  if (detailResp.data.code !== 0) return window.alert(detailResp.data.message || '读取报价明细失败')
  const main = mainResp.data.data || {}
  const details = detailResp.data.data || []
  quoteEditingId.value = Number(quoteId)
  quoteForm.value.contractId = main.contract_id ? String(main.contract_id) : ''
  lastQuoteContractId.value = quoteForm.value.contractId || ''
  quoteForm.value.assignmentId = main.assignment_id ? String(main.assignment_id) : ''
  quoteForm.value.operator = main.service_staff || main.created_by || '客服'
  quoteForm.value.quoteDesc = main.quote_desc || ''
  if (details.length > 0) quoteForm.value.category = categoryFromSavedDetails(details)
  quoteItems.value = details.length ? details.map((d) => quoteItemFromSavedDetail(d)) : [newQuoteItem()]
  quoteEditBaseline.value = currentQuoteEditFingerprint.value
  await loadQuoteAssignments()
  activeModule.value = C.quote
  activeSubModule.value = '报价明细'
}

function startEditQuoteByAssignment(assignmentRow) {
  const assignmentId = Number(assignmentRow?.id || 0)
  if (!assignmentId) return
  const hit = [...quoteOrderRows.value]
    .filter((q) => Number(q.assignment_id || 0) === assignmentId)
    .sort((a, b) => new Date(b.created_at || 0) - new Date(a.created_at || 0))[0]
  if (!hit) return window.alert('该深化工程师还没有可修改的报价单')
  startEditQuote(hit)
}

async function openQuoteLogs(row) {
  const quoteId = row.quote_id || row.quoteId || row.id
  if (!quoteId) return
  const { data } = await listQuoteLogs(quoteId)
  if (data.code !== 0) return window.alert(data.message || '读取日志失败')
  quoteLogsTarget.value = row || {}
  quoteLogsRows.value = data.data || []
  quoteLogsVisible.value = true
}

async function loadQuoteOrders() {
  const [orderResp, assignmentResp] = await Promise.all([
    listQuoteOrders(quoteOrderKeyword.value || ''),
    listQuoteAssignments(undefined, '')
  ])
  quoteOrderRows.value = orderResp?.data?.data || []
  quoteAllAssignments.value = assignmentResp?.data?.data || []
  if (quoteMainContractNo.value) {
    const exists = quoteOrderRows.value.some((r) => String(r.contract_no || '') === String(quoteMainContractNo.value))
      || quoteAllAssignments.value.some((a) => String(a.contract_no || '') === String(quoteMainContractNo.value))
    if (!exists) quoteMainContractNo.value = ''
  }
}

function financeExpectedAmount(orderRow) {
  const contractNo = String(orderRow?.contractNo || '')
  if (!contractNo) return 0
  return quoteOrderRows.value
    .filter(isSubmittedQuoteRow)
    .filter((q) => String(q.contract_no || '') === contractNo)
    .reduce((sum, q) => sum + Number(q.total_amount || 0), 0)
}

function financeForm(contractId) {
  const key = String(contractId)
  if (!financeCheckMap.value[key]) {
    financeCheckMap.value[key] = {
      amount: 0,
      payChannel: '',
      operator: '财务',
      remark: '',
      confirmed: false,
      allowPartialProduction: false,
      approver: '',
      approvalRemark: '',
      vouchers: []
    }
  }
  return financeCheckMap.value[key]
}

function financeGap(orderRow) {
  const expected = Number(financeExpectedAmount(orderRow) || 0)
  const actual = Number(financeForm(orderRow.id).amount || 0)
  return Math.max(0, Number((expected - actual).toFixed(2)))
}

async function loadFinanceVouchers(contractId) {
  const key = String(contractId)
  try {
    const { data } = await listFinanceVouchers(Number(contractId))
    financeForm(key).vouchers = data?.data || []
  } catch {
    financeForm(key).vouchers = []
  }
}

async function onFinanceVoucherUpload(orderRow, file) {
  if (!file) return
  if (file.size > 20 * 1024 * 1024) return window.alert('凭据文件不能超过20MB')
  const name = String(file.name || '').toLowerCase()
  const isPdf = name.endsWith('.pdf')
  const isImage = String(file.type || '').startsWith('image/')
  if (!isPdf && !isImage) return window.alert('仅支持图片或PDF文件')
  const form = financeForm(orderRow.id)
  const { data } = await uploadFinanceVoucher(orderRow.id, file, form.operator || '财务')
  if (data?.code !== 0) return window.alert(data?.message || '上传失败')
  await loadFinanceVouchers(orderRow.id)
}

function onFinanceVoucherFileChange(orderRow, evt) {
  const file = evt.target.files?.[0]
  evt.target.value = ''
  onFinanceVoucherUpload(orderRow, file)
}

function onDropFinanceVoucher(orderRow, evt) {
  const file = evt?.dataTransfer?.files?.[0]
  onFinanceVoucherUpload(orderRow, file)
}

function onPasteFinanceVoucher(orderRow, evt) {
  const items = evt?.clipboardData?.items
  if (!items || items.length === 0) return
  const imageItem = Array.from(items).find((it) => String(it.type || '').startsWith('image/'))
  if (!imageItem) return
  const file = imageItem.getAsFile?.()
  if (!file) return
  evt.preventDefault()
  onFinanceVoucherUpload(orderRow, file)
}

function openFinanceVoucherPreview(voucher) {
  const src = `/api/payments/vouchers/files/${voucher.id}`
  cadPreviewSrc.value = src
  cadScale.value = 1
  const name = String(voucher.file_name || '').toLowerCase()
  if (/\.(png|jpg|jpeg|webp|bmp|gif)$/.test(name)) cadPreviewType.value = 'image'
  else if (/\.pdf$/.test(name)) cadPreviewType.value = 'pdf'
  else cadPreviewType.value = 'other'
  cadPreviewVisible.value = true
}

async function removeFinanceVoucher(voucher) {
  if (!window.confirm(`确定删除凭据「${voucher.file_name || voucher.id}」吗？`)) return
  const { data } = await deleteFinanceVoucher(voucher.id)
  if (data?.code !== 0) return window.alert(data?.message || '删除失败')
  await loadFinanceVouchers(voucher.contract_id)
}

async function submitFinanceConfirm(orderRow) {
  const form = financeForm(orderRow.id)
  const expected = Number(financeExpectedAmount(orderRow).toFixed(2))
  const actual = Number(Number(form.amount || 0).toFixed(2))
  if (expected <= 0) {
    window.alert('该合同暂无应收金额（请先确认报价单已保存）')
    return false
  }
  if (!form.operator) {
    window.alert('请填写财务人员')
    return false
  }
  if (!form.confirmed) {
    window.alert('请勾选“已核对无误”后二次确认')
    return false
  }
  if (actual <= 0) {
    window.alert('请填写有效进账金额')
    return false
  }
  if (actual - expected > 0.01) {
    window.alert(`进账金额不能大于应收金额：应收 ${expected.toFixed(2)}，进账 ${actual.toFixed(2)}`)
    return false
  }
  const isFullPaid = Math.abs(actual - expected) <= 0.01
  const payStatus = isFullPaid ? '已到账' : '部分到账'
  const shortfall = Math.max(0, Number((expected - actual).toFixed(2)))
  const allowPartialProduction = !isFullPaid && !!form.allowPartialProduction
  if (allowPartialProduction && !String(form.approver || '').trim()) {
    window.alert('不足额排产需填写高权限审批人')
    return false
  }
  if (allowPartialProduction && !String(form.approvalRemark || '').trim()) {
    window.alert('不足额排产需填写审批原因')
    return false
  }
  if (allowPartialProduction && String(form.operator || '').trim() === String(form.approver || '').trim()) {
    window.alert('审批人不能与财务审核人相同，请由更高权限人员审批')
    return false
  }
  const trackSuffix = !isFullPaid ? `；差额待追踪 ${shortfall.toFixed(2)}` : ''
  const approvalSuffix = allowPartialProduction
    ? `；特批排产（审批人:${String(form.approver || '').trim()}，原因:${String(form.approvalRemark || '').trim()}）`
    : ''

  const payResp = await checkPayment({
    contractId: Number(orderRow.id),
    amount: actual,
    payStatus,
    payChannel: `${form.payChannel || ''}${trackSuffix}${approvalSuffix}`,
    operator: form.operator,
    customFields: JSON.stringify({
      expectedAmount: expected,
      actualAmount: actual,
      shortfallAmount: shortfall,
      isFullPaid,
      allowPartialProduction,
      financeRemark: String(form.remark || '').trim(),
      approver: String(form.approver || '').trim(),
      approvalRemark: String(form.approvalRemark || '').trim(),
      voucherIds: (form.vouchers || []).map((v) => v.id),
      savedAt: new Date().toISOString()
    }),
    customText1: String(form.approver || '').trim() || null,
    customText2: String(shortfall.toFixed(2)),
    customText3: isFullPaid ? '足额到账' : (allowPartialProduction ? '不足额特批排产' : '不足额待补款')
  })
  if (payResp?.data?.code !== 0) {
    window.alert(payResp?.data?.message || '财务核对失败')
    return false
  }

  if (!isFullPaid && !allowPartialProduction) {
    financeCommittedMap.value[String(orderRow.id)] = {
      category: 'partial',
      expected,
      actual,
      shortfall
    }
    form.confirmed = false
    await loadOrders()
    await loadQuoteOrders()
    window.alert(`已记录部分到账：应收 ${expected.toFixed(2)}，实收 ${actual.toFixed(2)}，待追踪 ${shortfall.toFixed(2)}`)
    return true
  }

  // 足额到账或高权限特批后，允许推进到“待排产”
  const statusRemark = isFullPaid
    ? `财务核对无误，足额到账，进入待排产${form.remark ? `；备注:${form.remark}` : ''}`
    : `不足额特批排产：应收${expected.toFixed(2)}，实收${actual.toFixed(2)}，待补${shortfall.toFixed(2)}；审批人:${String(form.approver || '').trim()}；原因:${String(form.approvalRemark || '').trim()}${form.remark ? `；备注:${form.remark}` : ''}`
  const statusResp = await updateContractStatus(orderRow.id, {
    targetStatus: '待排产',
    operator: form.operator,
    remark: statusRemark
  })
  if (statusResp?.data?.code !== 0) {
    window.alert(statusResp?.data?.message || '推进待排产失败')
    return false
  }
  form.confirmed = false
  form.allowPartialProduction = false
  form.approver = ''
  form.approvalRemark = ''
  financeCommittedMap.value[String(orderRow.id)] = {
    category: isFullPaid ? 'full' : 'partial',
    expected,
    actual,
    shortfall
  }
  await loadOrders()
  await loadQuoteOrders()
  if (isFullPaid) window.alert('审核完成，已足额到账，订单状态更新为“待排产”')
  else window.alert(`不足额已特批排产，待补金额 ${shortfall.toFixed(2)} 将持续留痕追踪`)
  return true
}

function refreshFinanceConfirmCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  financeConfirmCaptcha.value = Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

function openFinanceConfirm(orderRow) {
  financeConfirmTarget.value = orderRow
  financeConfirmInput.value = ''
  refreshFinanceConfirmCaptcha()
  financeConfirmVisible.value = true
}

async function confirmFinanceAudit() {
  if (!financeConfirmTarget.value) return
  if (String(financeConfirmInput.value || '').trim().toUpperCase() !== financeConfirmCaptcha.value) {
    financeConfirmInput.value = ''
    refreshFinanceConfirmCaptcha()
    return window.alert('验证码不正确，已刷新验证码')
  }
  const success = await submitFinanceConfirm(financeConfirmTarget.value)
  if (success) financeConfirmVisible.value = false
}

function selectedAssignment() {
  return quoteAssignments.value.find((a) => String(a.id) === String(quoteForm.value.assignmentId)) || null
}

function quoteAssignmentStatusText(status) {
  return ({
    QUOTED: '已报价',
    ASSIGNED: '已分配',
    PENDING_CLAIM: '待领取'
  })[String(status || '')] || status || '-'
}

function onAssignmentChange() {
  const a = selectedAssignment()
  if (!a) return
  quoteForm.value.operator = a.engineer || quoteForm.value.operator
  if (!assignmentForm.value.serviceStaff) assignmentForm.value.serviceStaff = a.service_staff || ''
  loadAssignmentQuoteForEdit(a)
}

function autoSelectQuoteAssignment() {
  if (!quoteAssignments.value.length) {
    quoteForm.value.assignmentId = ''
    return
  }
  const current = selectedAssignment()
  if (current) return
  if (quoteAssignments.value.length === 1) {
    quoteForm.value.assignmentId = String(quoteAssignments.value[0].id)
    onAssignmentChange()
  }
}

async function refreshQuoteSummary() {
  await loadQuoteOrders()
  if (quoteForm.value.contractId) {
    await loadQuoteAssignments()
  }
}

async function loadAssignmentQuoteForEdit(assignment) {
  const assignmentId = Number(assignment?.id || 0)
  if (!assignmentId) return
  const { data } = await listQuoteOrders('')
  const rows = data?.data || []
  const hit = rows
    .filter((q) => Number(q.assignment_id || 0) === assignmentId)
    .sort((x, y) => new Date(y.created_at || 0) - new Date(x.created_at || 0))[0]
  if (!hit) {
    quoteEditingId.value = null
    quoteItems.value = [newQuoteItem()]
    quoteEditBaseline.value = currentQuoteEditFingerprint.value
    return
  }
  const quoteId = hit.quote_id || hit.quoteId || hit.id
  const [mainResp, detailResp] = await Promise.all([getQuoteOrder(quoteId), listQuoteDetails(quoteId)])
  if (mainResp.data.code !== 0 || detailResp.data.code !== 0) return
  const main = mainResp.data.data || {}
  const details = detailResp.data.data || []
  quoteEditingId.value = Number(quoteId)
  quoteForm.value.contractId = main.contract_id ? String(main.contract_id) : quoteForm.value.contractId
  quoteForm.value.assignmentId = main.assignment_id ? String(main.assignment_id) : quoteForm.value.assignmentId
  quoteForm.value.operator = main.service_staff || main.created_by || quoteForm.value.operator
  quoteForm.value.quoteDesc = main.quote_desc || ''
  quoteItems.value = details.length ? details.map((d) => quoteItemFromSavedDetail(d)) : [newQuoteItem()]
  quoteEditBaseline.value = currentQuoteEditFingerprint.value
}

function addPendingEngineer() {
  const engineer = String(selectedEngineer.value || '').trim()
  if (!engineer) return window.alert('请先选择深化工程师')
  if (pendingAssignments.value.some((x) => x.engineer === engineer)) return window.alert('该工程师已在待分配列表')
  pendingAssignments.value.push({
    engineer,
    remark: assignmentForm.value.remark || ''
  })
  selectedEngineer.value = ''
  assignmentForm.value.remark = ''
}

async function loadQuoteAssignments() {
  if (!quoteForm.value.contractId) {
    quoteAssignments.value = []
    return
  }
  const { data } = await listQuoteAssignments(Number(quoteForm.value.contractId), assignmentKeyword.value || '')
  quoteAssignments.value = data?.data || []
  autoSelectQuoteAssignment()
}

async function loadQuotePool() {
  quotePoolLoading.value = true
  try {
    await loadOrders()
    const { data } = await listQuoteAssignments(undefined, '')
    const assignments = data?.data || []
    const orderMap = new Map((orderRows.value || []).map((o) => [String(o.id), o]))
    quotePoolRows.value = assignments.map((a) => ({
      ...a,
      order: orderMap.get(String(a.contract_id)) || null
    }))
  } finally {
    quotePoolLoading.value = false
  }
}

async function claimQuotePoolOrder(row) {
  const assignmentId = Number(row?.id || 0)
  const contractId = Number(row?.contract_id || 0)
  const engineer = String(loginUserName.value || '').trim()
  if (!assignmentId || !contractId) return window.alert('订单池记录缺少分配ID或订单ID')
  if (!engineer) return window.alert('请先登录深化工程师账号')
  const { data } = await updateQuoteAssignment(assignmentId, {
    contractId,
    serviceStaff: row.service_staff || '',
    engineer,
    status: '已分配',
    remark: row.remark || ''
  })
  if (data?.code !== 0) return window.alert(data?.message || '领取失败')
  activeModule.value = C.quote
  activeSubModule.value = '报价明细'
  quoteForm.value = { contractId: String(contractId), assignmentId: String(assignmentId), operator: engineer, quoteDesc: '', category: '' }
  assignmentKeyword.value = ''
  await searchContracts()
  await loadQuoteAssignments()
  quoteForm.value.assignmentId = String(assignmentId)
  onAssignmentChange()
  await loadQuotePool()
}

async function createAssignmentForCurrentContract() {
  if (!quoteForm.value.contractId) return window.alert('请先选择工厂订单号')
  if (pendingAssignments.value.length === 0) return window.alert('请先加入至少一个待分配工程师')
  const payloads = pendingAssignments.value.map((p) => ({
    contractId: Number(quoteForm.value.contractId),
    serviceStaff: assignmentForm.value.serviceStaff || '',
    engineer: p.engineer,
    status: '已分配',
    remark: p.remark || ''
  }))
  for (const payload of payloads) {
    const { data } = await createQuoteAssignment(payload)
    if (data.code !== 0) return window.alert(data.message || '创建分配失败')
  }
  await loadQuoteAssignments()
  pendingAssignments.value = []
  if (quoteAssignments.value.length > 0) {
    quoteForm.value.assignmentId = String(quoteAssignments.value[0].id)
    onAssignmentChange()
  }
  window.alert('分配创建成功')
}

async function openQuotePreview(row) {
  const quoteId = row.quote_id || row.quoteId || row.id
  if (!quoteId) return
  const [mainResp, detailResp] = await Promise.all([getQuoteOrder(quoteId), listQuoteDetails(quoteId)])
  if (mainResp.data.code !== 0) return window.alert(mainResp.data.message || '读取报价单失败')
  if (detailResp.data.code !== 0) return window.alert(detailResp.data.message || '读取报价明细失败')
  quotePreviewMain.value = mainResp.data.data || row || {}
  quotePreviewDetails.value = detailResp.data.data || []
  quotePreviewVisible.value = true
}

async function exportQuoteFromRow(row) {
  await openQuotePreview(row)
  exportQuoteExcel()
}

function openQuoteDelete(row) {
  quoteDeleteTarget.value = { ...row }
  quoteDeleteInput.value = ''
  refreshQuoteDeleteCaptcha()
  quoteDeleteVisible.value = true
}

function refreshQuoteDeleteCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  quoteDeleteCaptcha.value = Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

async function confirmQuoteDelete() {
  const quoteId = quoteDeleteTarget.value.quote_id || quoteDeleteTarget.value.quoteId || quoteDeleteTarget.value.id
  if (!quoteId) return window.alert('未找到报价单ID')
  if (String(quoteDeleteInput.value || '').trim().toUpperCase() !== quoteDeleteCaptcha.value) {
    refreshQuoteDeleteCaptcha()
    quoteDeleteInput.value = ''
    return window.alert('验证码不正确，已刷新验证码')
  }
  const name = quoteDeleteTarget.value.contract_no || quoteId
  if (!window.confirm(`请再次确认：确定永久删除报价单「${name}」吗？`)) return
  const { data } = await deleteQuoteOrder(quoteId)
  if (data.code !== 0) return window.alert(data.message || '删除失败')
  quoteDeleteVisible.value = false
  if (quotePreviewMain.value?.quote_id === quoteId) quotePreviewVisible.value = false
  await loadQuoteOrders()
  window.alert('报价单已删除')
}

function quoteFormulaForSaved(row) {
  const area = Number(row.area_m2 || 0)
  const base = Number(row.base_unit_price || 0)
  const baseAmount = Number((area * base).toFixed(2))
  const adjust = Number(row.special_adjust_total || 0)
  const amount = Number(row.amount || 0)
  const formula = [`基础 ${area.toFixed(4)}×${base}=${baseAmount.toFixed(2)}`]
  if (adjust) formula.push(`非标/额外 ${adjust.toFixed(2)}`)
  formula.push(`合计 ${amount.toFixed(2)}`)
  return formula.join('；')
}

async function exportQuoteExcel() {
  if (!quotePreviewMain.value?.quote_id) return window.alert('请先打开报价单')
  const filename = `${quotePreviewMain.value.contract_no || quotePreviewMain.value.quote_id}_报价单.xls`
  const html = await buildQuoteExcelHtml()
  const blob = new Blob([`\uFEFF${html}`], { type: 'application/vnd.ms-excel;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function buildQuoteExcelHtml() {
  const main = quotePreviewMain.value || {}
  const rows = quotePreviewDetails.value || []
  const logo = await getLogoDataUrl()
  const logoHtml = logo ? `<div style="margin:0 0 8px;text-align:left;"><img src="${logo}" alt="龙泽伟尼" style="height:42px;" /></div>` : ''
  const esc = (v) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const body = rows.map((row, idx) => `
    <tr>
      <td>${idx + 1}</td><td>${esc(row.product_name)}</td><td>${esc(row.material_structure)}</td>
      <td>${esc(row.handle_color || '')}</td><td>${esc(row.width_mm)}</td><td>${esc(row.height_mm)}</td><td>${esc(row.thickness_mm)}</td><td>${esc(row.quantity)}</td>
      <td>${esc(row.hinge_hole)}</td><td>${esc(row.process_desc)}</td><td>${esc(row.attachment_name)}</td><td>${esc(row.unit || 'm²')}</td>
      <td>${esc(row.area_m2)}</td><td>${esc(row.final_unit_price)}</td><td>${esc(row.amount)}</td><td>${esc(quoteFormulaForSaved(row))}</td><td></td><td></td>
    </tr>`).join('')
  const qtyTotal = rows.reduce((sum, row) => sum + Number(row.quantity || 0), 0)
  const areaTotal = rows.reduce((sum, row) => sum + Number(row.area_m2 || 0), 0).toFixed(4)
  return `
    <html><head><meta charset="UTF-8"><style>
      table{border-collapse:collapse;font-family:Microsoft YaHei,Arial;font-size:12px}
      th,td{border:1px solid #666;padding:5px;text-align:center}
      th{font-weight:bold;background:#f5f5f5}
      .title{font-size:18px;font-weight:bold}
      .total{background:#d9ffff;font-weight:bold}
    </style></head><body>
    ${logoHtml}
    <table>
      <tr><th>工厂订单号</th><td>${esc(main.contract_no)}</td><th>客户订单号</th><td>${esc(main.customer_order_no || '')}</td><th>接单日期</th><td>${esc(formatDateOnly(main.created_at))}</td><th>报价日期</th><td>${esc(formatDateOnly(main.created_at))}</td><th>报价员</th><td>${esc(main.created_by)}</td></tr>
      <tr><th>客服</th><td>${esc(main.service_staff)}</td><th>深化工程师</th><td>${esc(main.engineer)}</td><th>分配ID</th><td>${esc(main.assignment_id)}</td><th></th><td></td></tr>
      <tr><th>客户名称</th><td>${esc(main.customer_name)}</td><th>联系电话</th><td>${esc(main.customer_phone)}</td><th>发货地址</th><td colspan="3">${esc(main.customer_address)}</td></tr>
      <tr><th>特殊备注</th><td colspan="7">${esc(main.demand_desc || main.quote_desc || '')}</td></tr>
    </table>
    <br/>
    <table>
      <tr><th>序号</th><th>产品名称</th><th>材质结构</th><th>拉手颜色</th><th>宽</th><th>高</th><th>厚</th><th>数量</th><th>铰链孔</th><th>工艺说明</th><th>附图</th><th>单位</th><th>总量</th><th>单价</th><th>金额</th><th>备注/公式</th><th>生产工序</th><th>技术员</th></tr>
      ${body}
      <tr class="total"><td colspan="7">合计</td><td>${qtyTotal}</td><td colspan="4"></td><td>${areaTotal}</td><td></td><td>${esc(money(main.total_amount))}</td><td colspan="3"></td></tr>
    </table>
    </body></html>`
}

const logoDataUrlCache = ref('')
async function getLogoDataUrl() {
  if (logoDataUrlCache.value) return logoDataUrlCache.value
  try {
    const resp = await fetch(brandLogo)
    const blob = await resp.blob()
    const dataUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result || ''))
      reader.onerror = reject
      reader.readAsDataURL(blob)
    })
    logoDataUrlCache.value = String(dataUrl || '')
    return logoDataUrlCache.value
  } catch {
    return ''
  }
}

async function searchContracts() {
  const { data } = await listContracts()
  const all = data?.data || []
  const kw = String(contractKeyword.value || '').trim()
  if (!kw) {
    contractOptions.value = all
    return
  }
  contractOptions.value = all.filter((c) =>
    String(c.contractNo || '').includes(kw) ||
    String(c.factoryOrderNo || '').includes(kw) ||
    String(c.customerOrderNo || '').includes(kw) ||
    String(c.projectNo || '').includes(kw) ||
    String(c.projectName || c.projectNameSnapshot || '').includes(kw) ||
    String(c.customerName || '').includes(kw) ||
    String(c.customerPhone || '').includes(kw)
  )
}

async function onContractChange(evt) {
  lastQuoteContractId.value = quoteForm.value.contractId || ''
  quoteEditingId.value = null
  quoteEditBaseline.value = ''
  quoteItems.value = [newQuoteItem()]
  quoteForm.value.assignmentId = ''
  assignmentKeyword.value = ''
  assignmentForm.value = { serviceStaff: '', engineer: '', remark: '' }
  selectedEngineer.value = ''
  pendingAssignments.value = []
  quoteEditBaseline.value = currentQuoteEditFingerprint.value
  await loadQuoteAssignments()
  await loadCadFiles()
}

async function loadCadFiles() {
  if (!quoteForm.value.contractId) {
    cadFiles.value = []
    return
  }
  const { data } = await listContractCad(Number(quoteForm.value.contractId))
  cadFiles.value = data?.data || []
}

async function uploadCadFile(file) {
  if (!quoteForm.value.contractId) return window.alert('请先选择工厂订单号')
  if (!file) return
  try {
    await uploadContractCad(Number(quoteForm.value.contractId), file)
    await loadCadFiles()
    window.alert('CAD 文件上传成功')
  } catch (e) {
    window.alert(e?.response?.data?.message || '上传失败')
  }
}

function onCadFileSelect(evt) {
  const file = evt.target.files?.[0]
  evt.target.value = ''
  uploadCadFile(file)
}

function onDropCad(evt) {
  cadDrag.value = false
  const file = evt?.dataTransfer?.files?.[0]
  uploadCadFile(file)
}

function onPasteCad(evt) {
  const items = evt?.clipboardData?.items
  if (!items) return
  const imageItem = Array.from(items).find((item) => String(item.type || '').startsWith('image/'))
  if (!imageItem) return
  const file = imageItem.getAsFile?.()
  if (!file) return
  evt.preventDefault()
  uploadCadFile(file)
}

function openCadPreview(fileRow) {
  const name = String(fileRow.file_name || '').toLowerCase()
  const src = `/api/contracts/cad/files/${fileRow.id}`
  cadPreviewSrc.value = src
  cadScale.value = 1
  if (/\.(png|jpg|jpeg|webp|bmp|gif)$/.test(name)) cadPreviewType.value = 'image'
  else if (/\.pdf$/.test(name)) cadPreviewType.value = 'pdf'
  else cadPreviewType.value = 'other'
  cadPreviewVisible.value = true
}

function closeCadPreview() {
  cadPreviewVisible.value = false
  cadScale.value = 1
}

function zoomCad(step) {
  const next = Number((cadScale.value + step).toFixed(2))
  cadScale.value = Math.min(5, Math.max(0.2, next))
}

function onCadWheel(evt) {
  const step = evt.deltaY < 0 ? 0.1 : -0.1
  zoomCad(step)
}

function getCsvTemplate(moduleName) {
  const templates = {
    [C.staff]: 'staff_code,staff_name,role_type,process_name,phone,status,custom_fields\nYG001,张客服,客服,,13800000000,在职,{}',
    [C.product]: 'product_code,type,product_name,model,material_name,color,handle_color,unit_price,unit_price_unit,thickness,thickness_unit,size,image_url,status,custom_fields\nGMQS34XX26-GD,轻奢柜门,A轻奢柜门-34,23026,中纤,铣型拉手,本体固定,800,m2,26,mm,1200mm*2700mm,/api/master/files/product/demo.png,启用,{}',
    [C.materialData]: 'material_code,material_name,material_type,color,length_mm,width_mm,thickness_mm,image_url,unit,status,custom_fields\nCL001,芦花板,板材,浅胡桃,2440,1220,18,/api/master/files/demo_material.jpg,张,启用,{}',
  }
  return templates[moduleName] || ''
}

function exportCurrent() {
  if (activeModule.value !== C.master) {
    window.alert('导出暂时只开放给基础资料模块。')
    return
  }

  let rows = []
  let headers = []
  let filename = `${activeSubModule.value}_导出.csv`

  if (activeSubModule.value === C.staff) {
    rows = staffRows.value
    headers = ['staff_code', 'staff_name', 'role_type', 'process_name', 'phone', 'status', 'custom_fields']
  } else if (activeSubModule.value === C.product) {
    rows = productRows.value
    headers = ['product_code', 'type', 'product_name', 'model', 'material_name', 'color', 'handle_color', 'unit_price', 'unit_price_unit', 'thickness', 'thickness_unit', 'size', 'image_url', 'status', 'custom_fields']
  } else if (activeSubModule.value === C.materialData) {
    rows = materialRows.value
    headers = ['material_code', 'material_name', 'material_type', 'color', 'length_mm', 'width_mm', 'thickness_mm', 'image_url', 'status', 'custom_fields']
  } else {
    window.alert('当前页面暂无可导出数据。')
    return
  }

  const csv = toCsv(rows, headers)
  downloadTextFile(csv, filename)
}

function toCsv(rows, headers) {
  const head = headers.join(',')
  const body = rows.map((row) => headers.map((h) => csvEscape(row[h])).join(',')).join('\n')
  return `${head}\n${body}`
}

function csvEscape(value) {
  const text = value == null ? '' : String(value)
  if (text.includes(',') || text.includes('"') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function downloadTextFile(text, filename) {
  const blob = new Blob([`\uFEFF${text}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function applyClientFilter(list, filter, keywordFields, statusFields, colorField = 'color') {
  const kw = (filter.keyword || '').trim().toLowerCase()
  const st = (filter.status || '').trim().toLowerCase()
  const color = (filter.color || '').trim().toLowerCase()

  return list.filter((row) => {
    const passKeyword = !kw || keywordFields.some((f) => String(row[f] || '').toLowerCase().includes(kw))
    const passStatus = !st || statusFields.some((f) => String(row[f] || '').toLowerCase().includes(st))
    const passColor = !color || String(row[colorField] || '').toLowerCase().includes(color)
    return passKeyword && passStatus && passColor
  })
}

function toAbsUrl(url) {
  if (!url) return ''
  const normalized = String(url).trim().replace(/\\/g, '/')
  if (normalized.startsWith('blob:')) return normalized
  if (normalized.startsWith('http://') || normalized.startsWith('https://')) return normalized
  if (normalized.startsWith('/api/')) return normalized
  if (normalized.startsWith('api/')) return `/${normalized}`
  if (normalized.includes('/api/master/files/')) return normalized.substring(normalized.indexOf('/api/master/files/'))
  if (normalized.includes('/master/files/')) return `/api${normalized.substring(normalized.indexOf('/master/files/'))}`
  if (normalized.includes('upload/master/')) return `/api/master/files/${normalized.split('/').pop()}`
  return `/api/master/files/${normalized.split('/').pop()}`
}

function formatDateTime(value) {
  if (!value) return '-'
  const text = String(value).replace('T', ' ')
  return text.length > 19 ? text.slice(0, 19) : text
}

function formatDateOnly(value) {
  const text = formatDateTime(value)
  return text === '-' ? '-' : text.slice(0, 10)
}

function money(value) {
  const n = Number(value || 0)
  return n.toFixed(2)
}

function onImageError(evt) {
  const fallback = 'data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"96\" height=\"96\"><rect width=\"100%\" height=\"100%\" fill=\"%230b2a47\"/><text x=\"50%\" y=\"50%\" fill=\"%238fb6d9\" font-size=\"14\" text-anchor=\"middle\" dominant-baseline=\"middle\">暂无</text></svg>'
  if (evt.target.dataset.fallbackApplied === '1') return
  evt.target.dataset.fallbackApplied = '1'
  evt.target.src = fallback
}

async function onUploadProductImage(productId, file) {
  if (!file) return
  if (!String(file.type || '').startsWith('image/')) {
    window.alert('请选择图片文件')
    return
  }
  try {
    await uploadProductImage(productId, file)
    await refreshMasterData()
    window.alert('产品图片上传成功')
  } catch (e) {
    try {
      const tmp = await uploadMasterImageTemp('product', file)
      const url = tmp?.data?.data
      if (!url) throw new Error('上传返回为空')
      const ret = await updateMaster(C.product, productId, { image_url: url })
      if (ret?.data?.code !== 0) throw new Error(ret?.data?.message || '回写失败')
      await refreshMasterData()
      window.alert('产品图片上传成功')
    } catch (e2) {
      const msg = e2?.response?.data?.message || e?.response?.data?.message || e2?.message || e?.message || '产品图片上传失败'
      window.alert(msg)
    }
  }
}

async function onUploadMaterialImage(materialId, file) {
  if (!file) return
  if (!String(file.type || '').startsWith('image/')) {
    window.alert('请选择图片文件')
    return
  }
  try {
    await uploadMaterialImage(materialId, file)
    await refreshMasterData()
    window.alert('材料图片上传成功')
  } catch (e) {
    try {
      const tmp = await uploadMasterImageTemp('material', file)
      const url = tmp?.data?.data
      if (!url) throw new Error('上传返回为空')
      const ret = await updateMaster(C.materialData, materialId, { image_url: url })
      if (ret?.data?.code !== 0) throw new Error(ret?.data?.message || '回写失败')
      await refreshMasterData()
      window.alert('材料图片上传成功')
    } catch (e2) {
      const msg = e2?.response?.data?.message || e?.response?.data?.message || e2?.message || e?.message || '材料图片上传失败'
      window.alert(msg)
    }
  }
}

function onProductFileChange(productId, evt) {
  const file = evt.target.files?.[0]
  evt.target.value = ''
  onUploadProductImage(productId, file)
}

async function onDropRowImage(type, id, evt) {
  const file = getImageFileFromDrop(evt)
  if (!file) return
  if (type === 'product') await onUploadProductImage(id, file)
  else await onUploadMaterialImage(id, file)
}

async function onPasteRowImage(type, id, evt) {
  const file = getImageFileFromPaste(evt)
  if (!file) return
  if (type === 'product') await onUploadProductImage(id, file)
  else await onUploadMaterialImage(id, file)
}

function getImageFileFromDrop(evt) {
  const files = evt?.dataTransfer?.files
  if (!files || files.length === 0) return null
  const imageFile = Array.from(files).find((f) => String(f.type || '').startsWith('image/'))
  if (!imageFile) {
    window.alert('拖入的不是图片文件')
    return null
  }
  return imageFile
}

function getImageFileFromPaste(evt) {
  const items = evt?.clipboardData?.items
  if (!items || items.length === 0) return null
  const imageItem = Array.from(items).find((item) => String(item.type || '').startsWith('image/'))
  if (!imageItem) return null
  const file = imageItem.getAsFile?.()
  if (!file) return null
  evt.preventDefault()
  return file
}

function onMaterialFileChange(materialId, evt) {
  const file = evt.target.files?.[0]
  evt.target.value = ''
  onUploadMaterialImage(materialId, file)
}

async function removeRowImage(module, id) {
  const { data } = await updateMaster(module, id, { image_url: '' })
  if (data.code !== 0) return window.alert(data.message)
  await refreshMasterData()
}

function downloadTemplate() {
  const map = {
    [C.staff]: '/import-templates/人员管理_导入模板.csv',
    [C.product]: '/import-templates/产品资料_导入模板.csv',
    [C.materialData]: '/import-templates/材料资料_导入模板.csv',
  }
  const path = map[activeSubModule.value]
  if (!path) { window.alert('当前页面暂未提供模板下载，请切换到基础资料子页面。'); return }
  window.open(path, '_blank')
}

onMounted(async () => {
  window.addEventListener('keydown', onGlobalKeydown)
  window.addEventListener('beforeunload', onBeforeUnload)
  clearAuthToken()
  lastQuoteContractId.value = quoteForm.value.contractId || ''
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
  window.removeEventListener('beforeunload', onBeforeUnload)
  if (undoTimer) clearTimeout(undoTimer)
  if (globalSearchTimer) clearTimeout(globalSearchTimer)
})

let undoTimer = null
let globalSearchTimer = null
watch(
  [quoteForm, quoteItems, assignmentForm, pendingAssignments, selectedEngineer, orderForm, addStaff, addProduct, addMaterial, editData, financeCheckMap],
  () => {
    if (restoringUndo.value) return
    if (undoTimer) clearTimeout(undoTimer)
    undoTimer = setTimeout(() => {
      pushUndoSnapshot()
      undoTimer = null
    }, 250)
  },
  { deep: true }
)

watch(globalSearch, () => {
  if (globalSearchTimer) clearTimeout(globalSearchTimer)
  globalSearchTimer = setTimeout(() => {
    applyGlobalSearch()
    globalSearchTimer = null
  }, 260)
})

watch(
  () => masterContractForm.value.masterContractNo,
  (val) => {
    const no = String(val || '').trim()
    if (!no) return
    if (orderCreateStep.value !== 2) return
    autofillOrderFormByMasterNo(no)
  }
)
</script>

<style scoped>
.erp-layout { min-height: 100vh; display: flex; background: #f7fafc; color: #1f2937; font-family: 'Microsoft YaHei UI', sans-serif; }
.project-order-title { display: grid; gap: 4px; }
.project-order-title strong { font-size: 18px; color: #10203c; }
.project-order-title span { color: #64748b; font-size: 13px; }
.inline-field { display: flex; gap: 8px; align-items: center; }
.inline-field select { min-width: 0; flex: 1; }
.inline-project-panel { margin: 12px 0 18px; padding: 14px 16px; border: 1px solid #dbe5f1; border-radius: 10px; background: #f8fbff; }
.inline-project-panel .actions { margin-top: 10px; }
.link-btn { border: 0; background: transparent; color: #2563eb; padding: 0; font-weight: 700; cursor: pointer; }
.project-detail-grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 10px; margin-bottom: 12px; }
.project-detail-grid > div { min-height: 64px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 8px; background: #f8fbff; display: grid; gap: 5px; }
.project-detail-grid span { color: #64748b; font-size: 12px; }
.project-detail-grid strong { color: #13213b; }
.project-tabs { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 12px; }
.project-tabs button { border: 1px solid #dbe4f2; background: #fff; color: #40516d; border-radius: 8px; padding: 7px 10px; font-weight: 700; }
.project-tabs button.active { background: #eef4ff; border-color: #9dbbf8; color: #2353b8; }
.project-placeholder { padding: 24px; border: 1px dashed #cbd5e1; border-radius: 8px; background: #fbfdff; }
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #ececec;
}
.login-card { width: min(1280px, 94vw); min-height: min(780px, 90vh); background: #fff; border-radius: 24px; box-shadow: 0 24px 64px rgba(15, 23, 42, 0.16); overflow: hidden; display: grid; grid-template-columns: 1.5fr 1fr; }
.login-visual { background-size: cover; background-position: center; position: relative; }
.login-form-wrap { padding: clamp(36px, 3.6vw, 56px); display: grid; align-content: center; background: #f7f7f8; }
.login-right-logo { width: min(300px, 96%); height: auto; margin-bottom: 20px; }
.login-title { font-size: clamp(26px, 2.1vw, 40px); font-weight: 800; color: #2b313b; text-align: left; letter-spacing: .5px; }
.login-sub { font-size: clamp(16px, 1.25vw, 22px); color: #8b919b; text-align: left; margin-top: 8px; margin-bottom: 20px; }
.login-form { display: grid; gap: 14px; }
.login-input { display: grid; grid-template-columns: 26px 1fr; align-items: center; gap: 10px; background: #fff; border: 1px solid #e6e7eb; border-radius: 12px; padding: 0 14px; box-shadow: 0 3px 10px rgba(15, 23, 42, .04); }
.login-input-icon { display: inline-flex; align-items: center; justify-content: center; color: #8e96a3; }
.login-input-icon svg { width: 20px; height: 20px; display: block; }
.login-form input { min-width: 0; width: 100%; box-sizing: border-box; height: 56px; font-size: 24px; padding: 0; border: 0; outline: 0; background: transparent; color: #3b4350; }
.login-form input::placeholder { color: #9aa1ab; }
.login-btn.primary { width: 100%; margin-top: 10px; height: 62px; font-size: clamp(22px, 1.8vw, 32px); border-radius: 12px; background: #f9c800; border: 0; color: #2f3440; font-weight: 800; letter-spacing: .5px; box-shadow: 0 8px 18px rgba(249, 200, 0, .28); }
.login-btn.primary:hover { filter: brightness(.98); }
.login-safe-note { margin-top: 14px; font-size: clamp(15px, 1vw, 20px); color: #959ca7; text-align: center; padding-top: 16px; border-top: 1px solid #e5e7eb; }
@media (max-width: 900px) {
  .login-page { padding: 10px; }
  .login-card { grid-template-columns: 1fr; min-height: 0; border-radius: 14px; }
  .login-visual { min-height: 320px; }
  .login-form-wrap { padding: 20px 16px 18px; }
  .login-right-logo { width: min(260px, 85%); }
  .login-title { font-size: 28px; }
  .login-sub { font-size: 16px; margin: 8px 0 14px; }
  .login-form { gap: 12px; }
  .login-form input { height: 50px; font-size: 16px; }
  .login-btn { height: 52px; font-size: 20px; }
  .login-safe-note { font-size: 13px; padding-top: 12px; }
}
.left-nav { width: 260px; background: linear-gradient(180deg, #1f5fae 0%, #114084 100%); border-right: 1px solid #1d4e89; padding: 14px; }
.brand { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.brand-logo { width: 120px; height: auto; border-radius: 4px; background: #fff; padding: 2px 4px; }
.logo-dot { width: 12px; height: 12px; border-radius: 50%; background: #10d7ff; box-shadow: 0 0 12px #10d7ff; }
.brand-title { font-size: 16px; font-weight: 700; }
.brand-sub { font-size: 12px; color: #d8e8ff; }
.menu-search input { width: 100%; border: 1px solid rgba(255, 255, 255, 0.35); background: rgba(255, 255, 255, 0.18); color: #f8fbff; border-radius: 8px; padding: 8px 10px; }
.menu-group { margin-top: 10px; }
.module-btn { width: 100%; display: flex; justify-content: space-between; align-items: center; border: 0; border-radius: 8px; padding: 8px 10px; color: #dbeafe; background: transparent; cursor: pointer; }
.module-btn.active, .module-btn:hover { background: rgba(255, 255, 255, 0.2); color: #ffffff; }
.badge { background: rgba(255, 255, 255, 0.2); color: #ffffff; font-size: 12px; padding: 1px 8px; border-radius: 12px; }
.submenu { margin: 4px 0 2px 10px; border-left: 1px solid rgba(255, 255, 255, 0.35); }
.sub-btn { display: block; width: calc(100% - 8px); margin: 5px 0 5px 8px; text-align: left; border: 0; border-radius: 6px; padding: 7px 10px; background: transparent; color: #dbeafe; cursor: pointer; }
.sub-btn.active, .sub-btn:hover { background: rgba(255, 255, 255, 0.25); color: #ffffff; }
.workspace { flex: 1; padding: 16px; }
.workspace, .card, .dialog-sm, .import-dialog { text-align: center; }
.customer-profile-host { text-align: left; }
.topbar { display: flex; justify-content: space-between; align-items: center; gap: 10px; background: #ffffff; border: 1px solid #dbe5f1; border-radius: 10px; padding: 14px; box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06); }
.topbar h1 { margin: 0; font-size: 20px; color: #0f172a; }
.topbar p { margin: 4px 0 0; color: #64748b; font-size: 12px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.login-user { display: inline-flex; align-items: center; font-size: 12px; color: #334155; padding: 0 4px; }
.actions.end { justify-content: flex-end; }
button { border: 0; border-radius: 8px; padding: 8px 12px; cursor: pointer; font: inherit; }
.ghost { background: #ffffff; color: #1e3a8a; border: 1px solid #bfdbfe; }
.primary { background: linear-gradient(90deg, #2563eb, #0ea5e9); color: #ffffff; font-weight: 700; }
.mini { padding: 4px 8px; font-size: 12px; margin-right: 6px; }
.danger { background: #8b1f2f; color: #fff; }
.status-pill { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; border: 1px solid transparent; }
.status-draft { color: #ffd166; background: rgba(255, 209, 102, 0.14); border-color: rgba(255, 209, 102, 0.4); }
.status-enabled { color: #4ade80; background: rgba(74, 222, 128, 0.14); border-color: rgba(74, 222, 128, 0.4); }
.status-disabled { color: #f87171; background: rgba(248, 113, 113, 0.14); border-color: rgba(248, 113, 113, 0.4); }
.status-select { min-width: 86px; width: 86px; padding: 3px 8px; border-radius: 14px; font-size: 12px; font-weight: 700; text-align: center; }
.status-select.status-draft { color: #ffd166; background: rgba(255, 209, 102, 0.14); border-color: rgba(255, 209, 102, 0.4); }
.status-select.status-enabled { color: #4ade80; background: rgba(74, 222, 128, 0.14); border-color: rgba(74, 222, 128, 0.4); }
.status-select.status-disabled { color: #f87171; background: rgba(248, 113, 113, 0.14); border-color: rgba(248, 113, 113, 0.4); }
.order-quote { color: #7dd3fc; background: rgba(125, 211, 252, 0.16); border-color: rgba(125, 211, 252, 0.45); }
.order-confirm { color: #fcd34d; background: rgba(252, 211, 77, 0.16); border-color: rgba(252, 211, 77, 0.45); }
.order-pay { color: #fdba74; background: rgba(253, 186, 116, 0.16); border-color: rgba(253, 186, 116, 0.45); }
.order-paid { color: #34d399; background: rgba(52, 211, 153, 0.16); border-color: rgba(52, 211, 153, 0.45); }
.order-scheduled { color: #a78bfa; background: rgba(167, 139, 250, 0.16); border-color: rgba(167, 139, 250, 0.45); }
.order-producing { color: #22d3ee; background: rgba(34, 211, 238, 0.16); border-color: rgba(34, 211, 238, 0.45); }
.order-finished { color: #4ade80; background: rgba(74, 222, 128, 0.16); border-color: rgba(74, 222, 128, 0.45); }
.order-ship-wait { color: #f9a8d4; background: rgba(249, 168, 212, 0.16); border-color: rgba(249, 168, 212, 0.45); }
.order-shipped { color: #e5e7eb; background: rgba(229, 231, 235, 0.16); border-color: rgba(229, 231, 235, 0.45); }
.card { margin-top: 12px; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 12px; box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04); }
.table-tools { display: flex; gap: 8px; margin-bottom: 10px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.labeled-tools { align-items: end; flex-wrap: wrap; }
.labeled-grid .field-box { background: #f8fafc; border: 1px solid #dbe5f1; border-radius: 8px; padding: 8px; }
.field-box { text-align: left; }
.field-box label { display: block; font-size: 12px; color: #475569; margin-bottom: 6px; font-weight: 600; }
.field-box.compact { min-width: 220px; }
input, select, textarea { border: 1px solid #cbd5e1; background: #ffffff; color: #0f172a; border-radius: 8px; padding: 8px 10px; }
input { min-width: 260px; }
input[type="checkbox"] { min-width: 16px; width: 16px; height: 16px; padding: 0; vertical-align: middle; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th, td { border: 1px solid #dbe5f1; padding: 7px; text-align: center; vertical-align: middle; }
th { background: #eff6ff; color: #1e3a8a; }
.batch-col { width: 38px; min-width: 38px; max-width: 38px; padding: 4px 2px; }
.batch-col input[type="checkbox"] { width: 14px; height: 14px; min-width: 14px; margin: 0; }
pre { background: rgba(5, 18, 32, 0.9); border: 1px solid rgba(57, 120, 182, 0.35); border-radius: 8px; padding: 10px; overflow: auto; }
.img-cell { display: flex; align-items: center; gap: 8px; }
.drop-paste-zone { border: 1px dashed rgba(93, 164, 223, 0.45); border-radius: 8px; padding: 6px; }
.drop-paste-zone:focus { outline: none; border-color: #10d7ff; box-shadow: 0 0 0 2px rgba(16, 215, 255, 0.2); }
.upload-tip { font-size: 12px; color: #475569; }
.thumb { width: 56px; height: 56px; object-fit: cover; border-radius: 6px; border: 1px solid #cbd5e1; background: #f8fafc; }
.empty-thumb { display: inline-flex; align-items: center; justify-content: center; color: #64748b; font-size: 12px; }
.mini-upload { position: relative; display: inline-flex; padding: 4px 8px; border-radius: 6px; font-size: 12px; background: #ffffff; border: 1px solid #cbd5e1; cursor: pointer; color: #1e3a8a; }
.mini-upload input { display: none; }
.import-mask { position: fixed; inset: 0; background: rgba(3, 9, 17, 0.7); display: flex; align-items: center; justify-content: center; z-index: 9; }
.import-dialog { width: min(900px, 96vw); background: #ffffff; border: 1px solid #dbe5f1; border-radius: 12px; padding: 14px; color: #0f172a; }
.dialog-sm { width: min(760px, 96vw); background: #ffffff; border: 1px solid #dbe5f1; border-radius: 12px; padding: 14px; color: #0f172a; }
.quote-preview-dialog { width: min(1320px, 96vw); max-height: 92vh; overflow: auto; background: #ffffff; border: 1px solid #dbe5f1; border-radius: 12px; padding: 14px; color: #0f172a; }
.quote-preview-sheet { background: #fff; color: #111827; padding: 8px; overflow: auto; }
.quote-sheet-logo-wrap { margin: 0 0 8px; }
.quote-sheet-logo { height: 42px; width: auto; display: block; }
.quote-preview-sheet table { color: #111827; font-size: 12px; }
.quote-preview-sheet th, .quote-preview-sheet td { border: 1px solid #6b7280; padding: 5px 6px; color: #111827; background: #fff; }
.quote-preview-sheet th { background: #f8fafc; color: #111827; font-weight: 800; }
.quote-preview-meta { min-width: 1120px; margin-bottom: 0; }
.quote-preview-meta th { width: 88px; background: #fff7ed; }
.quote-preview-meta td { min-width: 130px; text-align: left; }
.quote-preview-lines { min-width: 1220px; }
.quote-preview-lines th { background: #ecfeff; }
.quote-preview-lines td { white-space: normal; }
.quote-preview-lines .left-text { text-align: left; line-height: 1.4; }
.quote-total-row td { background: #cffafe !important; font-weight: 800; }
.delete-warning { text-align: left; padding: 10px; border-radius: 8px; border: 1px solid #fecaca; background: #fff1f2; color: #9f1239; margin-bottom: 10px; }
.delete-warning p { margin: 6px 0 0; color: #be123c; }
.captcha-row { display: flex; align-items: center; gap: 8px; }
.captcha-code { letter-spacing: 4px; font-weight: 900; color: #031521; background: linear-gradient(135deg, #fef3c7, #67e8f9); border-radius: 8px; padding: 8px 12px; user-select: none; min-width: 120px; text-align: center; }
.detail-grid > div { background: #f8fafc; border: 1px solid #dbe5f1; border-radius: 8px; padding: 8px; color: #0f172a; }
.detail-grid label { display: block; font-size: 12px; color: #475569; margin-bottom: 6px; }
.detail-grid > div > div { color: #0f172a; }
.detail-image-wrap { display: grid; gap: 6px; justify-items: center; }
.detail-thumb { width: 96px; height: 96px; }
.previewable { cursor: zoom-in; }
.img-path { font-size: 11px; color: #64748b; word-break: break-all; }
.image-edit-grid { grid-template-columns: 1fr; }
.preview-dialog { width: min(1100px, 96vw); max-height: 92vh; background: #ffffff; border: 1px solid #dbe5f1; border-radius: 12px; padding: 12px; display: flex; justify-content: center; align-items: center; }
.preview-image { max-width: 100%; max-height: 86vh; object-fit: contain; border-radius: 8px; transition: transform 0.08s linear; transform-origin: center center; }
.import-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.close-btn { background: transparent; color: #334155; font-size: 20px; padding: 0 8px; }
.import-toolbar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.file-btn { position: relative; display: inline-flex; align-items: center; padding: 8px 12px; border-radius: 8px; background: #ffffff; color: #1e3a8a; border: 1px solid #cbd5e1; cursor: pointer; }
.file-btn input { display: none; }
.drop-upload { border: 1px dashed #93c5fd; border-radius: 8px; padding: 10px; display: grid; gap: 8px; align-items: center; justify-items: start; color: #475569; background: #f8fbff; }
.drop-upload.dragover { border-color: #2563eb; background: #e0f2fe; color: #1e3a8a; }
.drop-upload:focus { outline: none; border-color: #10d7ff; box-shadow: 0 0 0 2px rgba(16, 215, 255, 0.2); }
.import-box { width: 100%; min-height: 200px; box-sizing: border-box; }
.hint { color: #64748b; font-size: 12px; }
.template-box { margin-top: 8px; }
.rule-list { margin-top: 4px; max-height: 140px; overflow: auto; border: 1px solid #cbd5e1; border-radius: 6px; padding: 4px; text-align: left; background: #ffffff; }
.rule-item { display: block; font-size: 12px; color: #0f172a; margin: 2px 0; }
.rule-pick-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 6px 4px; border-bottom: 1px solid #e2e8f0; }
.rule-pick-row:last-child { border-bottom: 0; }
.rule-pick-row .mini { background: #eff6ff; color: #1e3a8a; border: 1px solid #bfdbfe; }
.rule-pick-row .mini:disabled { background: #f1f5f9; color: #94a3b8; border-color: #e2e8f0; cursor: not-allowed; }
.selected-rule-list { display: flex; gap: 6px; flex-wrap: wrap; }
.selected-rule { display: inline-flex; align-items: center; gap: 5px; padding: 3px 8px; border-radius: 12px; color: #166534; background: #dcfce7; border: 1px solid #86efac; font-size: 12px; }
.selected-rule button { padding: 0 4px; color: #166534; background: transparent; }
.table-scroll { width: 100%; max-height: 62vh; overflow: auto; }
.model-cell { position: relative; min-width: 220px; }
.model-options { position: absolute; left: 0; right: 0; top: 100%; z-index: 8; background: #ffffff; border: 1px solid #cbd5e1; border-radius: 6px; max-height: 180px; overflow: auto; text-align: left; }
.model-option { padding: 6px 8px; cursor: pointer; border-bottom: 1px solid rgba(63,118,173,.2); }
.model-option:hover { background: #eff6ff; }
.cad-panel { margin: 10px 0; padding: 10px; border: 1px solid rgba(63,118,173,.3); border-radius: 8px; }
.cad-title { font-weight: 700; margin-bottom: 8px; color: #bce9ff; }
.cad-list { margin-top: 8px; display: grid; gap: 6px; text-align: left; }
.cad-item { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 6px 8px; border: 1px solid rgba(63,118,173,.2); border-radius: 6px; }
.cad-preview-dialog { flex-direction: column; align-items: center; }
.cad-pdf { width: 90vw; height: 78vh; border: 1px solid rgba(63,118,173,.3); border-radius: 6px; background: #fff; transform-origin: center center; }
.quote-empty { padding: 28px 12px; border: 1px dashed #bfdbfe; border-radius: 8px; background: #f8fbff; }
.quote-empty-title { color: #1e3a8a; font-weight: 700; margin-bottom: 6px; }
.quote-sheet-head { display: grid; grid-template-columns: repeat(5, minmax(150px, 1fr)); border: 1px solid #cbd5e1; border-bottom: 0; background: #eef6ff; }
.sheet-cell { min-height: 58px; padding: 8px; border-right: 1px solid #cbd5e1; border-bottom: 1px solid #cbd5e1; text-align: center; display: grid; align-content: center; gap: 4px; }
.sheet-cell span { color: #475569; font-size: 12px; }
.sheet-cell strong { color: #0f172a; font-size: 13px; word-break: break-word; }
.quote-category-tabs { display: flex; gap: 6px; flex-wrap: wrap; margin: 10px 0; }
.category-tab-wrap { display: inline-flex; align-items: center; gap: 4px; }
.category-tab { padding: 7px 14px; border-radius: 4px; background: rgba(250, 181, 114, 0.22); color: #ffd7ad; border: 1px solid rgba(250, 181, 114, 0.55); }
.category-tab.active { background: #f7b26a; color: #17202b; font-weight: 700; }
.quote-category-actions { display: flex; justify-content: flex-end; gap: 8px; margin: 0 0 8px; }
.quote-sheet-scroll { width: 100%; overflow: auto; border: 1px solid rgba(63,118,173,.35); }
.quote-sheet-table { min-width: 1320px; table-layout: fixed; }
.quote-sheet-table th { background: #f1f5f9; color: #1e3a8a; }
.quote-sheet-table td { background: #ffffff; }
.quote-sheet-table input:not([type="checkbox"]), .quote-sheet-table select { min-width: 92px; width: 100%; box-sizing: border-box; padding: 6px 8px; border-radius: 4px; }
.quote-select-col { width: 48px; text-align: center; padding: 4px 2px; font-size: 12px; color: #334155; }
.quote-select-col input[type="checkbox"] { width: 14px; height: 14px; min-width: 14px; margin: 0; }
.formula-row td { background: #e0f2fe; text-align: left; color: #0f172a; line-height: 1.45; white-space: normal; }
.selected-rule input { width: 70px; min-width: 70px; padding: 2px 5px; }
.nonstandard-row td { background: rgba(255, 233, 233, 0.08); }
.nonstandard-panel { display: grid; gap: 8px; text-align: left; }
.custom-rule-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.quote-card-list { display: grid; gap: 10px; }
.quote-card { border: 1px solid rgba(63,118,173,.35); border-radius: 10px; padding: 10px; background: rgba(7, 24, 42, 0.55); text-align: left; }
.quote-card-title { font-weight: 700; color: #bce9ff; margin-bottom: 8px; }
.quote-amount { color: #7dd3fc; font-weight: 700; margin-right: 10px; }
.assignment-pending { margin: 8px 0; border: 1px solid #dbe5f1; border-radius: 8px; padding: 8px; background: #f8fafc; }
.assignment-pending-title { text-align: left; color: #1e3a8a; font-weight: 700; margin-bottom: 6px; }
.assignment-board { margin: 8px 0 10px; border: 1px solid #dbe5f1; border-radius: 8px; padding: 8px; background: #ffffff; }
.assignment-board-title { text-align: left; color: #1e3a8a; font-weight: 700; margin-bottom: 6px; }
.assignment-active { background: #eff6ff; }
.assignment-progress { display: inline-block; margin-left: 8px; padding: 2px 8px; border-radius: 12px; background: #e0f2fe; color: #075985; font-weight: 800; cursor: help; }
.rule-list-wrap { margin-top: 8px; }
.template-title { color: #9ad8ff; margin-bottom: 6px; }
.summary-dialog { width: min(1080px, 96vw); }
.quote-main-order-info { display: grid; grid-template-columns: repeat(6, minmax(120px, 1fr)); gap: 8px; margin: 8px 0 10px; }
.quote-main-order-info > div { background: #f8fafc; border: 1px solid #dbe5f1; border-radius: 8px; padding: 8px; text-align: center; }
.quote-main-order-info span { display: block; font-size: 12px; color: #475569; margin-bottom: 4px; }
.quote-main-order-info strong { color: #0f172a; font-size: 14px; word-break: break-word; }
.quote-main-order-info .amount-emphasis { background: #fff7ed; border-color: #fdba74; }
.quote-main-order-info .amount-emphasis strong { color: #b91c1c; font-size: 20px; font-weight: 800; }
.quote-amount-emphasis { color: #b91c1c !important; font-weight: 900; font-size: 16px; background: #fff1f2; border: 1px solid #fecaca; border-radius: 6px; padding: 2px 8px; display: inline-block; }
.partial-input { border-color: #ef4444 !important; background: #fff1f2 !important; color: #b91c1c !important; font-weight: 700; }
.finance-gap-emphasis { color: #dc2626; font-weight: 800; background: #fff1f2; border: 1px solid #fecaca; border-radius: 6px; padding: 2px 8px; display: inline-block; }
.finance-confirm-cell { min-width: 130px; }
.finance-confirm-label { display: inline-flex; align-items: center; gap: 6px; justify-content: center; white-space: nowrap; font-size: 13px; margin: 2px 0; }
.finance-confirm-label.partial-approve { color: #92400e; font-weight: 700; }
.finance-approver-input { min-width: 160px; width: 160px; padding: 4px 6px; margin: 2px auto; display: block; }
.finance-voucher-cell { min-width: 260px; }
.voucher-drop { padding: 6px; justify-items: center; }
.voucher-list { margin-top: 6px; display: grid; gap: 4px; }
.voucher-item { display: flex; align-items: center; gap: 6px; justify-content: center; flex-wrap: wrap; }
.voucher-name { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; color: #1e3a8a; }
.finance-group-row td { text-align: left; font-weight: 800; color: #1e3a8a; background: #eef6ff; }
.summary-amount-cell { color: #b91c1c !important; font-size: 18px; font-weight: 900; background: #fff1f2 !important; }

/* ===== UI rollback + refine (layout + sidebar + table) ===== */
.erp-layout {
  min-height: 100vh;
  position: relative;
  display: block;
  background: #f5f6f8;
  color: #111827;
  font-family: Inter, "PingFang SC", "Microsoft YaHei UI", sans-serif;
}
.left-nav {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 20;
  width: 220px;
  background: #111827;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  padding: 16px 12px;
  transition: all .3s ease-in-out;
  overflow-x: hidden;
}

.left-nav.collapsed { width: 72px; }
.workspace {
  margin-left: 220px;
  padding: 24px;
  transition: margin-left .3s ease-in-out;
}
.erp-layout.collapsed .workspace { margin-left: 72px; }

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 48px;
  margin-bottom: 18px;
  overflow: hidden;
}
.brand-logo {
  width: 104px;
  height: 34px;
  object-fit: contain;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e5e7eb;
  padding: 2px 6px;
  flex: 0 0 auto;
}
.brand-logo.is-collapsed {
  width: 36px;
  height: 36px;
  padding: 3px;
}
.brand-text {
  overflow: hidden;
  white-space: nowrap;
}
.brand-title { color: #f5f5f5; font-size: 14px; font-weight: 600; line-height: 1.2; }
.brand-sub { color: #a3a3a3; font-size: 11px; margin-top: 2px; }
.collapse-btn {
  margin-left: auto;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: #f5f5f5;
}
.menu-search input {
  width: 100%;
  min-width: 0;
  height: 40px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: #f5f5f5;
}

.module-btn {
  width: 100%;
  min-height: 40px;
  border-radius: 999px;
  color: rgba(245, 245, 245, 0.72);
  background: transparent;
  border: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  position: relative;
}
.module-btn-main { display: inline-flex; align-items: center; gap: 10px; }
.module-icon { width: 20px; text-align: center; opacity: .9; }
.module-name { color: inherit; }
.module-btn:hover { background: rgba(255, 255, 255, 0.08); color: #fff; }
.module-btn.disabled { opacity: .42; cursor: not-allowed; }
.module-btn.disabled:hover { background: transparent; color: inherit; }
.module-btn.active {
  background: rgba(246, 180, 0, 0.16);
  color: #fff;
  box-shadow: inset 3px 0 0 #f6b400, 0 10px 26px rgba(246, 180, 0, 0.12);
}
.module-btn.icononly {
  width: 44px;
  height: 44px;
  min-height: 44px;
  margin: 0 auto;
  padding: 0;
  justify-content: center;
  border-radius: 12px;
}
.module-btn.icononly.active {
  background: rgba(246, 180, 0, 0.16);
  box-shadow: inset 0 0 0 2px rgba(246, 180, 0, 0.42);
}
.badge {
  background: rgba(255, 255, 255, 0.16);
  color: #f5f5f5;
  border-radius: 999px;
  font-size: 11px;
  padding: 1px 8px;
}
.badge-dot {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  border-radius: 999px;
  background: #f6b400;
  color: #111827;
  font-size: 10px;
}
.submenu { margin: 6px 0 2px 10px; border-left: 1px solid rgba(255, 255, 255, 0.2); }
.sub-btn { color: #d4d4d4; border-radius: 10px; padding: 8px 10px; }
.sub-btn.active, .sub-btn:hover { background: rgba(255, 255, 255, 0.14); color: #fff; }

.left-nav:not(.collapsed)::after {
  content: "LONZVINE\\A全屋定制 · 整家服务 · 品质保障";
  white-space: pre-line;
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 16px;
  min-height: 86px;
  border-radius: 18px;
  padding: 16px;
  color: rgba(255,255,255,.82);
  font-size: 12px;
  line-height: 1.8;
  background: linear-gradient(145deg, rgba(246,180,0,.18), rgba(255,255,255,.05));
  box-shadow: inset 0 0 0 1px rgba(255,255,255,.08);
}

.topbar {
  display: grid;
  grid-template-columns: minmax(220px, auto) minmax(260px, 420px) minmax(max-content, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 76px;
  padding: 12px 14px;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}
.topbar-left { grid-column: 1; text-align: left; }
.topbar h1 { margin: 0; font-size: 22px; font-weight: 600; }
.crumb { margin: 2px 0 0; color: #6b7280; font-size: 12px; }
.sys-ok { margin-top: 4px; }
.top-search { grid-column: 2; }
.top-search input {
  width: 100%;
  min-width: 0;
  height: 40px;
  border-radius: 14px;
  border: 1px solid #dde2ea;
  background: #fff;
}
.top-search input:focus,
.table-tools input:focus,
input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: rgba(245, 180, 0, 0.55);
  box-shadow: 0 0 0 3px rgba(245, 180, 0, 0.15);
}
.topbar > .actions {
  grid-column: 3;
  justify-self: end;
}
.actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card {
  margin-top: 14px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e8eaf0;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.06);
  padding: 24px;
}

button { transition: all .2s ease; }
.primary {
  background: #f5b400;
  color: #111827;
  font-weight: 600;
  border: 1px solid #e6aa00;
  border-radius: 14px;
  height: 40px;
  padding: 0 14px;
  box-shadow: 0 8px 20px rgba(245, 180, 0, 0.25);
}
.primary:hover { filter: brightness(.96); transform: translateY(-1px); }
.primary:active { transform: scale(.98); }
.ghost {
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #374151;
  border-radius: 12px;
  height: 40px;
  padding: 0 12px;
}
.ghost:hover { background: #f9fafb; }

.table-tools { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; align-items: center; }
.table-tools input {
  height: 40px;
  border-radius: 14px;
  border: 1px solid #dde2ea;
  min-width: 220px;
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 14px;
}
th, td {
  border: 0;
  border-bottom: 1px solid #eef0f4;
  padding: 0 8px;
  height: 52px;
  color: #1f2937;
}
th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}
tbody tr:hover { background: #fff8e1; }
tbody tr:nth-child(even):hover { background: #f9fafb; }

.status-enabled { color: #15803d; background: #dcfce7; border-color: #bbf7d0; }
.status-disabled { color: #6b7280; background: #f3f4f6; border-color: #e5e7eb; }

.mini {
  border-radius: 8px;
  padding: 4px 8px;
  margin-right: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
}
.mini[title='查看'],
.mini[title='详情'],
.mini[title='编辑'],
.mini[title='删除'] {
  width: 32px;
  height: 32px;
  padding: 0;
  font-size: 0;
  position: relative;
}
.mini[title='查看']::before,
.mini[title='详情']::before,
.mini[title='编辑']::before,
.mini[title='删除']::before {
  font-size: 14px;
  line-height: 1;
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
}
.mini[title='查看']::before, .mini[title='详情']::before { content: '👁'; }
.mini[title='编辑']::before, .mini[title='修改']::before { content: '✎'; }
.mini[title='删除']::before { content: '🗑'; }
.mini:hover { background: #f8fafc; }
.mini.danger { color: #ef4444; border-color: #fecaca; }
.mini.danger:hover { background: #fff1f2; }

.login-btn.primary { background: #f5b400; color: #111827; }

.contract-layout {
  max-width: 1280px;
  margin: 0 auto;
}
.contract-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 18px;
  box-sizing: border-box;
}
.contract-op-card {
  margin-bottom: 10px;
  padding: 20px 24px;
}
.contract-op-grid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.project-order-title {
  min-width: 0;
  text-align: left;
  display: grid;
  gap: 4px;
}
.project-order-title strong {
  color: #111827;
  font-size: 22px;
  line-height: 1.2;
}
.project-order-title span {
  color: #64748b;
  font-size: 13px;
}
.contract-op-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  flex: 0 0 auto;
}
.contract-op-actions button {
  height: 40px;
  border-radius: 12px;
  padding: 0 16px;
  font-weight: 700;
  white-space: nowrap;
}
.contract-filter-card {
  margin-bottom: 14px;
  padding: 14px 16px;
}
.contract-main-card {
  margin-bottom: 14px;
}
.section-title {
  text-align: left;
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
  margin: 2px 0 12px;
}
.contract-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 24px;
  row-gap: 18px;
  margin-bottom: 18px;
}
.form-item {
  width: 100%;
  box-sizing: border-box;
  text-align: left;
}
.form-item label {
  display: block;
  margin-bottom: 6px;
  color: #1f2937;
  font-size: 12px;
  font-weight: 600;
}
.form-item input,
.form-item select,
.form-item textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d8dee9;
  border-radius: 10px;
  background: #fff;
  color: #1f2937;
}
.form-item input,
.form-item select {
  height: 40px;
}
.form-item textarea {
  min-height: 88px;
  padding-top: 8px;
  resize: vertical;
}
.form-item span.required-star {
  color: #ef4444;
}
.form-item.span-2 {
  grid-column: span 2;
}
.contract-list-tools {
  max-width: none;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}
.contract-list-tools input {
  flex: 1 1 420px;
  min-width: 260px;
  height: 42px;
  border-radius: 12px;
}
.contract-pending-toggle {
  height: 42px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f9fafb;
  color: #374151;
  font-weight: 600;
  white-space: nowrap;
}
.contract-pending-toggle input {
  width: 16px;
  height: 16px;
  min-width: 16px;
}
.customer-picker {
  position: relative;
}
.customer-dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 6px);
  z-index: 30;
  max-height: 236px;
  overflow-y: auto;
  border: 1px solid #dbe5f1;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 16px 34px rgba(15, 23, 42, .14);
  padding: 6px;
}
.customer-dropdown button {
  width: 100%;
  height: 44px;
  display: grid;
  align-items: center;
  text-align: left;
  border: 0;
  border-radius: 9px;
  background: transparent;
  padding: 7px 10px;
  color: #111827;
}
.customer-dropdown button:hover {
  background: #fff7e0;
}
.customer-dropdown strong {
  font-size: 14px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.customer-dropdown span {
  color: #64748b;
  font-size: 12px;
}
.required-star {
  color: #ef4444;
  font-weight: 700;
}
.contract-bottom-bar {
  position: sticky;
  bottom: 10px;
  z-index: 2;
  background: rgba(255,255,255,.95);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(15,23,42,.08);
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.contract-bottom-bar span {
  color: #6b7280;
  font-size: 13px;
}
.contract-bottom-bar > div {
  display: flex;
  gap: 8px;
}
.prefix-switch {
  display: inline-grid;
  grid-template-columns: repeat(2, 42px);
  gap: 6px;
  flex: 0 0 auto;
}
.prefix-switch button {
  height: 40px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background: #fff;
  color: #374151;
  font-weight: 800;
}
.prefix-switch button.active {
  background: #f5b400;
  border-color: #e6aa00;
  color: #111827;
  box-shadow: 0 6px 14px rgba(245, 180, 0, .22);
}
.compact-table {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.compact-table th,
.compact-table td {
  height: 38px;
  padding: 6px 10px;
}

@media (max-width: 980px) {
  .left-nav { width: 72px; }
  .workspace { margin-left: 72px; padding: 12px; }
  .topbar { grid-template-columns: 1fr; }
  .topbar-left,
  .top-search,
  .topbar > .actions { grid-column: auto; justify-self: stretch; }
  .actions { margin-top: 6px; justify-content: flex-start; }
  .form-grid { grid-template-columns: 1fr; }
  .quote-main-order-info { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .contract-form-grid { grid-template-columns: 1fr; }
  .form-item.span-2 { grid-column: span 1; }
  .contract-op-grid,
  .contract-list-tools { flex-direction: column; align-items: stretch; }
  .contract-op-actions { justify-content: flex-start; }
  .contract-list-tools input { min-width: 0; flex-basis: auto; }
  .contract-bottom-bar { position: static; }
}
@media (max-width: 900px) {
  .contract-form-grid { grid-template-columns: 1fr; }
}
</style>

