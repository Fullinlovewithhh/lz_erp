<template>
  <div class="craft-dropdown">
    <label class="craft-label">工艺搜索</label>
    <div class="craft-picker">
      <div class="craft-input-wrap">
        <input
          :value="keyword"
          placeholder="搜索特殊工艺"
          @input="handleInput"
          @keydown.esc="isOpen = false"
        />
        <button type="button" class="craft-toggle" @click="toggleList">
          {{ isOpen ? '▲' : '▼' }}
        </button>
      </div>

      <Transition name="craft-menu">
        <div v-if="isOpen" class="craft-menu">
          <div v-if="filteredOptions.length" class="craft-list">
            <button
              v-for="rule in filteredOptions"
              :key="rule.id"
              type="button"
              class="craft-option"
              :disabled="selectedRuleIds.includes(rule.id)"
              @click="selectRule(rule)"
            >
              <span class="craft-name">{{ ruleName(rule) }}</span>
              <span class="craft-meta">{{ ruleText(rule) }}</span>
              <span v-if="selectedRuleIds.includes(rule.id)" class="craft-added">已加入</span>
            </button>
          </div>
          <div v-else class="craft-empty">暂无匹配工艺</div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  keyword: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  selectedIds: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:keyword', 'search', 'select'])

const isOpen = ref(false)

const selectedRuleIds = computed(() => props.selectedIds || [])

const filteredOptions = computed(() => {
  const keyword = String(props.keyword || '').trim().toLowerCase()
  if (!keyword) return props.options || []
  return (props.options || []).filter((rule) => {
    const fields = [
      ruleName(rule),
      modeText(rule.adjust_mode || rule.adjustMode),
      rule.unit_desc || rule.unitDesc || '',
    ]
    return fields.some((text) => String(text || '').toLowerCase().includes(keyword))
  })
})

function handleInput(event) {
  emit('update:keyword', event.target.value)
  isOpen.value = true
  emit('search')
}

function toggleList() {
  if (isOpen.value) {
    isOpen.value = false
    return
  }
  openList(true)
}

function openList(shouldSearch) {
  isOpen.value = true
  if (shouldSearch) emit('search')
}

function selectRule(rule) {
  if (selectedRuleIds.value.includes(rule.id)) return
  emit('select', rule)
  isOpen.value = false
}

function ruleName(rule) {
  return rule.rule_name || rule.ruleName || ''
}

function ruleText(rule) {
  const mode = modeText(rule.adjust_mode || rule.adjustMode)
  const value = Number(rule.adjust_value ?? rule.adjustValue ?? 0)
  const unit = rule.unit_desc || rule.unitDesc || ((rule.adjust_mode || rule.adjustMode) === 'PERCENT' ? '%' : '元/平方')
  return `${mode} / ${value}${unit}`
}

function modeText(mode) {
  if (mode === 'PERCENT') return '按百分比'
  if (mode === 'FIXED_PER_ITEM') return '按件'
  return '按平米'
}
</script>

<style scoped>
.craft-dropdown {
  display: grid;
  grid-template-columns: 90px minmax(220px, 520px);
  align-items: start;
  gap: 8px;
  max-width: 640px;
}
.craft-label {
  height: 34px;
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #596b82;
}
.craft-picker {
  position: relative;
}
.craft-input-wrap {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  height: 34px;
  border: 1px solid #d7e1ec;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}
.craft-input-wrap:focus-within {
  border-color: #9dc5f8;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, .09);
}
.craft-input-wrap input {
  min-width: 0;
  height: 32px;
  border: 0;
  outline: none;
  background: transparent;
  padding: 0 10px;
}
.craft-toggle {
  border: 0;
  border-left: 1px solid #e3ebf4;
  background: #f8fbff;
  color: #51647d;
  cursor: pointer;
  font-size: 11px;
}
.craft-toggle:hover {
  background: #edf6ff;
  color: #1f5f9e;
}
.craft-menu {
  position: absolute;
  top: 40px;
  left: 0;
  right: 0;
  z-index: 20;
  background: #fff;
  border: 1px solid #d8e3ef;
  border-radius: 7px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, .10);
  padding: 6px;
}
.craft-list {
  max-height: 240px;
  overflow-y: auto;
  display: grid;
  gap: 4px;
  padding-right: 2px;
}
.craft-option {
  width: 100%;
  border: 0;
  background: #fff;
  border-radius: 6px;
  padding: 8px 9px;
  display: grid;
  grid-template-columns: minmax(120px, 1fr) auto auto;
  gap: 8px;
  align-items: center;
  text-align: left;
  color: #243247;
  cursor: pointer;
}
.craft-option:hover {
  background: #eef6ff;
}
.craft-option:disabled {
  cursor: default;
  color: #94a3b8;
  background: #f8fafc;
}
.craft-name {
  font-weight: 600;
}
.craft-meta {
  color: #64748b;
  font-size: 12px;
}
.craft-added {
  color: #2563eb;
  font-size: 12px;
}
.craft-empty {
  color: #94a3b8;
  padding: 12px;
  text-align: center;
}
.craft-menu-enter-active,
.craft-menu-leave-active {
  transition: opacity .16s ease, transform .16s ease;
}
.craft-menu-enter-from,
.craft-menu-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
@media (max-width: 900px) {
  .craft-dropdown {
    grid-template-columns: 1fr;
  }
  .craft-label {
    height: auto;
  }
}
</style>
