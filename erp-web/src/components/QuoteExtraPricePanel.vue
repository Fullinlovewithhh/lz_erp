<template>
  <Transition name="extra-panel">
    <div class="extra-price-panel">
      <div class="extra-panel-head">
        <div>
          <div class="extra-title">非标加价详情</div>
          <div class="extra-subtitle">当前报价明细的加价项目</div>
        </div>
        <div class="head-right">
          <div class="extra-total">非标合计 {{ money(extraTotal) }}</div>
          <button class="ghost tiny" @click="toggleCollapsed">{{ collapsed ? '展开详情' : '收起详情' }}</button>
        </div>
      </div>

      <div class="extra-section">
        <div class="extra-section-title">已添加的非标项目列表</div>
        <div v-if="selectedItems.length" class="extra-chip-list">
          <div v-for="item in selectedItems" :key="item.key" class="extra-chip">
            <div class="chip-main">
              <span class="chip-name">{{ item.name }}</span>
              <span class="chip-rule">{{ item.ruleText }}</span>
              <span class="chip-formula">{{ item.formulaText }}</span>
              <template v-if="isEditing(item)">
                <div class="chip-edit"><span>名称</span><input v-model="editState[item.key].ruleName" /></div>
                <div class="chip-edit"><span>单位</span><input v-model="editState[item.key].unitDesc" /></div>
                <div class="chip-edit"><span>调价值</span><input v-model.number="editState[item.key].adjustValue" type="number" /></div>
                <div class="chip-edit"><span>数量</span><input v-model.number="editState[item.key].ruleQuantity" type="number" min="1" /></div>
              </template>
              <div v-if="showQtyEditor(item.rule) && !isEditing(item)" class="chip-edit">
                <span>数量</span>
                <input :value="ruleQty(item.rule)" type="number" min="1" @input="updateRuleQty(item, $event)" />
              </div>
              <div v-if="item.type === 'custom' && !isEditing(item)" class="chip-edit">
                <span>调价值</span>
                <input :value="ruleAdjust(item.rule)" type="number" @input="updateRuleAdjust(item, $event)" />
              </div>
            </div>
            <div class="chip-actions">
              <button v-if="!isEditing(item)" class="mini" @click="startEdit(item)">编辑</button>
              <button v-if="isEditing(item)" class="mini" @click="saveEdit(item)">保存</button>
              <button v-if="isEditing(item)" class="mini ghost" @click="cancelEdit(item)">取消</button>
            <button class="soft-danger" @click="removeItem(item)">删除</button>
            </div>
          </div>
        </div>
        <div v-else class="extra-empty">暂无非标加价项目</div>
      </div>

      <div class="extra-section" v-if="!collapsed">
        <div class="extra-section-title">特殊工艺搜索/新增区域</div>
        <SpecialCraftDropdown
          v-model:keyword="row.ruleKeyword"
          :options="row.ruleOptions"
          :selected-ids="row.selectedRuleIds"
          @search="$emit('search-rules')"
          @select="$emit('add-rule', $event)"
        />
      </div>

      <div class="extra-section" v-if="!collapsed">
        <div class="extra-section-title">新增自定义非标项目</div>
        <div class="extra-form-grid">
          <div class="field-box"><label>工艺名称</label><input v-model="row.customRule.ruleName" placeholder="如：异形拼花" /></div>
          <div class="field-box"><label>计价方式</label><select v-model="row.customRule.adjustMode">
            <option value="FIXED_PER_M2">按平米加减</option>
            <option value="PERCENT">按百分比加减</option>
            <option value="FIXED_PER_ITEM">按件加减</option>
          </select></div>
          <div class="field-box"><label>调价值</label><input v-model.number="row.customRule.adjustValue" type="number" /></div>
          <div class="field-box"><label>单位说明</label><input v-model="row.customRule.unitDesc" placeholder="元/平方" /></div>
          <div class="field-box"><label>最低费用</label><input v-model.number="row.customRule.minCharge" type="number" /></div>
          <div class="field-box"><label>最高费用</label><input v-model.number="row.customRule.maxCharge" type="number" /></div>
        </div>
        <div class="extra-actions">
          <button class="primary" @click="$emit('add-custom')">加入本行</button>
          <button class="ghost" @click="$emit('save-custom')">入库</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, reactive } from 'vue'
import SpecialCraftDropdown from './SpecialCraftDropdown.vue'

const props = defineProps({
  row: { type: Object, required: true },
  selectedRules: { type: Array, default: () => [] },
  estimateCharge: { type: Function, required: true },
  ruleFormula: { type: Function, required: true },
})

const emit = defineEmits(['search-rules', 'add-rule', 'remove-rule', 'remove-custom', 'add-custom', 'save-custom', 'rule-change', 'request-remove-item'])

function ruleName(rule) {
  return rule.rule_name || rule.ruleName || ''
}

function ruleText(rule) {
  const value = Number(rule.adjust_value ?? rule.adjustValue ?? 0)
  const unit = rule.unit_desc || rule.unitDesc || (rule.adjustMode === 'PERCENT' || rule.adjust_mode === 'PERCENT' ? '%' : '元/平方')
  return `${value}${unit}`
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

const selectedItems = computed(() => [
  ...(props.selectedRules || []).map((rule) => ({
    type: 'rule',
    key: `rule_${rule.id}`,
    id: rule.id,
    rule,
    name: ruleName(rule),
    ruleText: ruleText(rule),
    formulaText: `${props.ruleFormula(rule)}，当前加价 ${money(props.estimateCharge(rule))}`,
  })),
  ...((props.row.customRules || []).map((rule, index) => ({
    type: 'custom',
    key: `custom_${index}_${rule.ruleName}`,
    index,
    rule,
    name: ruleName(rule),
    ruleText: ruleText(rule),
    formulaText: `${props.ruleFormula(rule)}，当前加价 ${money(props.estimateCharge(rule))}`,
  }))),
])

const extraTotal = computed(() => selectedItems.value.reduce((sum, item) => {
  const rule = item.type === 'rule'
    ? props.selectedRules.find((r) => r.id === item.id)
    : props.row.customRules[item.index]
  return sum + Number(props.estimateCharge(rule) || 0)
}, 0))

const collapsed = computed(() => !!props.row.extraPanelCollapsed)

function toggleCollapsed() {
  props.row.extraPanelCollapsed = !props.row.extraPanelCollapsed
}

function removeItem(item) {
  emit('request-remove-item', item)
}

const editState = reactive({})

function startEdit(item) {
  editState[item.key] = {
    ruleName: item.rule.rule_name || item.rule.ruleName || '',
    unitDesc: item.rule.unit_desc || item.rule.unitDesc || '',
    adjustValue: Number(item.rule.adjust_value ?? item.rule.adjustValue ?? 0),
    ruleQuantity: Number(item.rule.ruleQuantity || item.rule.rule_quantity || 1),
  }
}

function cancelEdit(item) {
  delete editState[item.key]
}

function isEditing(item) {
  return !!editState[item.key]
}

function saveEdit(item) {
  const form = editState[item.key]
  if (!form) return
  const name = String(form.ruleName || '').trim()
  if (!name) return
  item.rule.rule_name = name
  item.rule.ruleName = name
  item.rule.unit_desc = String(form.unitDesc || '').trim()
  item.rule.unitDesc = String(form.unitDesc || '').trim()
  item.rule.adjust_value = Number(form.adjustValue || 0)
  item.rule.adjustValue = Number(form.adjustValue || 0)
  const qty = Math.max(1, Number(form.ruleQuantity || 1))
  item.rule.ruleQuantity = qty
  item.rule.rule_quantity = qty
  emit('rule-change')
  cancelEdit(item)
}

function showQtyEditor(rule) {
  const mode = rule.adjust_mode || rule.adjustMode
  const unit = String(rule.unit_desc || rule.unitDesc || '')
  return mode === 'FIXED_PER_ITEM' || unit.includes('个') || unit.includes('块') || unit.includes('件') || unit.includes('把')
}

function ruleQty(rule) {
  const v = Number(rule.ruleQuantity || rule.rule_quantity || 1)
  return v > 0 ? v : 1
}

function ruleAdjust(rule) {
  return Number(rule.adjustValue ?? rule.adjust_value ?? 0)
}

function updateRuleQty(item, evt) {
  const value = Math.max(1, Number(evt?.target?.value || 1))
  item.rule.ruleQuantity = value
  item.rule.rule_quantity = value
  emit('rule-change')
}

function updateRuleAdjust(item, evt) {
  const value = Number(evt?.target?.value || 0)
  item.rule.adjustValue = value
  item.rule.adjust_value = value
  emit('rule-change')
}
</script>

<style scoped>
.extra-panel-enter-active, .extra-panel-leave-active { transition: all .22s ease; overflow: hidden; }
.extra-panel-enter-from, .extra-panel-leave-to { opacity: 0; transform: translateY(-4px); max-height: 0; }
.extra-panel-enter-to, .extra-panel-leave-from { opacity: 1; transform: translateY(0); max-height: 620px; }
.extra-price-panel {
  text-align: left;
  background: #f8fbff;
  border: 1px solid #dbe7f3;
  border-radius: 8px;
  padding: 12px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.8), 0 6px 18px rgba(15, 23, 42, .05);
}
.extra-panel-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 10px; border-bottom: 1px solid #e5edf6; }
.head-right { display: flex; align-items: center; gap: 8px; }
.extra-title { font-weight: 700; color: #1f2a44; }
.extra-subtitle { font-size: 12px; color: #718096; margin-top: 2px; }
.extra-total { font-size: 12px; color: #1f3b61; background: #edf6ff; border: 1px solid #d6e8fb; padding: 5px 9px; border-radius: 999px; }
.extra-section { padding-top: 10px; }
.extra-section-title { font-size: 12px; font-weight: 700; color: #40536d; margin-bottom: 8px; }
.extra-chip-list { display: grid; gap: 8px; }
.extra-chip { display: flex; align-items: center; justify-content: space-between; gap: 10px; background: #fff; border: 1px solid #dfe8f2; border-radius: 7px; padding: 8px 10px; }
.chip-main { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.chip-actions { display: inline-flex; align-items: center; gap: 6px; }
.chip-name { font-weight: 700; color: #1f2937; }
.chip-rule { color: #2563eb; background: #eef5ff; border: 1px solid #d7e8ff; border-radius: 999px; padding: 2px 7px; font-size: 12px; }
.chip-formula { color: #64748b; font-size: 12px; }
.chip-edit { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: #475569; }
.chip-edit input { width: 72px; height: 28px; border: 1px solid #d7e1ec; border-radius: 6px; padding: 0 6px; }
.extra-empty { color: #94a3b8; background: #fff; border: 1px dashed #cbd5e1; border-radius: 7px; padding: 10px; }
.field-box label { font-size: 12px; color: #596b82; }
.field-box input, .field-box select { width: 100%; height: 34px; border: 1px solid #d7e1ec; border-radius: 6px; background: #fff; padding: 0 8px; }
.extra-form-grid { display: grid; grid-template-columns: repeat(3, minmax(120px, 1fr)); gap: 8px; }
.extra-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 10px; }
.soft-danger { border: 1px solid #fecaca; color: #b91c1c; background: #fff7f7; border-radius: 6px; padding: 4px 9px; font-size: 12px; }
.soft-danger:hover { background: #fee2e2; color: #991b1b; }
.tiny { padding: 4px 8px; font-size: 12px; }
@media (max-width: 900px) {
  .extra-form-grid { grid-template-columns: 1fr; }
}
</style>
