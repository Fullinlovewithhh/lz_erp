import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

let authToken = ''

export function setAuthToken(token) {
  authToken = String(token || '').trim()
}

export function clearAuthToken() {
  authToken = ''
}

http.interceptors.request.use((config) => {
  const token = authToken
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function login(payload) {
  return http.post('/auth/login', payload)
}

export function me() {
  return http.get('/auth/me')
}

export function logout() {
  return http.post('/auth/logout')
}

export function listAuthUsers(keyword) {
  return http.get('/auth/users', { params: { keyword } })
}

export function createAuthUser(payload) {
  return http.post('/auth/users', payload)
}

export function updateAuthUser(id, payload) {
  return http.put(`/auth/users/${id}`, payload)
}

export function resetAuthUserPassword(id, newPassword) {
  return http.put(`/auth/users/${id}/password`, { newPassword })
}

export function createContract(payload) {
  return http.post('/contracts', payload)
}

export function listContracts(status) {
  return http.get('/contracts', { params: { status } })
}

export function updateContractStatus(id, payload) {
  return http.put(`/contracts/${id}/status`, payload)
}

export function updateContract(id, payload) {
  return http.put(`/contracts/${id}`, payload)
}

export function deleteContract(id) {
  return http.delete(`/contracts/${id}`)
}

export function listContractLogs(id) {
  return http.get(`/contracts/${id}/logs`)
}
export function uploadContractCad(contractId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/contracts/${contractId}/cad`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export function listContractCad(contractId) {
  return http.get(`/contracts/${contractId}/cad`)
}

export function createQuote(payload) {
  return http.post('/quotes', payload)
}

export function checkPayment(payload) {
  return http.post('/payments/check', payload)
}

export function uploadFinanceVoucher(contractId, file, operator) {
  const formData = new FormData()
  formData.append('file', file)
  if (operator) formData.append('operator', operator)
  return http.post(`/payments/${contractId}/vouchers`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listFinanceVouchers(contractId) {
  return http.get(`/payments/${contractId}/vouchers`)
}

export function deleteFinanceVoucher(id) {
  return http.delete(`/payments/vouchers/${id}`)
}

export function createProduction(payload) {
  return http.post('/productions', payload)
}

export function updateProductionStatus(payload) {
  return http.put('/productions/status', payload)
}

export function listProductions(contractId) {
  return http.get('/productions', { params: { contractId } })
}

export function createCustomer(payload) {
  return http.post('/customers', payload)
}

export function updateCustomer(id, payload) {
  return http.put(`/customers/${id}`, payload)
}

export function deleteCustomer(id) {
  return http.delete(`/customers/${id}`)
}

export function listCustomers(keyword) {
  return http.get('/customers', { params: { keyword } })
}

export function createProject(payload) {
  return http.post('/projects', payload)
}

export function listProjects(customerId, keyword) {
  return http.get('/projects', { params: { customerId, keyword } })
}

export function getProject(id) {
  return http.get(`/projects/${id}`)
}

export function deleteProject(id) {
  return http.delete(`/projects/${id}`)
}

export function createCustomerOrderWithFiles(payload, files) {
  const formData = new FormData()
  formData.append('customerId', payload.customerId)
  formData.append('customerOrderNo', payload.customerOrderNo)
  formData.append('remark', payload.remark || '')
  Array.from(files || []).forEach(file => formData.append('files', file))
  return http.post('/customer-orders/with-files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listCustomerOrders(customerId, keyword) {
  return http.get('/customer-orders', { params: { customerId, keyword } })
}

export function createCustomerFollowup(payload) {
  return http.post('/customers/followups', payload)
}

export function listCustomerFollowups(customerId) {
  return http.get(`/customers/${customerId}/followups`)
}

export function createCustomerRequirement(payload) {
  return http.post('/customers/requirements', payload)
}

export function listCustomerRequirements(customerId) {
  return http.get(`/customers/${customerId}/requirements`)
}

export function listProducts(keyword) {
  return http.get('/master/products', { params: { keyword } })
}

export function listMaterials(keyword) {
  return http.get('/master/materials', { params: { keyword } })
}

export function listStaff(keyword) {
  return http.get('/master/staff', { params: { keyword } })
}

export function batchImport(module, rows) {
  return http.post(`/import/${module}`, { ignoreDuplicate: true, rows })
}

export function uploadProductImage(productId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/master/products/${productId}/image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadMaterialImage(materialId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/master/materials/${materialId}/image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadMasterImageTemp(module, file) {
  const formData = new FormData()
  formData.append('module', module)
  formData.append('file', file)
  return http.post('/master/uploads/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getMasterDetail(module, id) {
  return http.get(`/master/${module}/${id}`)
}

export function updateMaster(module, id, payload) {
  return http.put(`/master/${module}/${id}`, payload)
}

export function deleteMaster(module, id) {
  return http.delete(`/master/${module}/${id}`)
}

export function listQuoteRules(keyword, category) {
  return http.get('/quote-rules', { params: { keyword, category } })
}

export function createQuoteRule(payload) {
  return http.post('/quote-rules', payload)
}

export function updateQuoteRule(id, payload) {
  return http.put(`/quote-rules/${id}`, payload)
}

export function deleteQuoteRule(id) {
  return http.delete(`/quote-rules/${id}`)
}

export function calculateQuoteDetail(payload) {
  return http.post('/quote-details/calculate', payload)
}

export function saveQuoteDetails(payload) {
  return http.post('/quote-details/save', payload)
}

export function listQuoteOrders(keyword) {
  return http.get('/quote-details/quotes', { params: { keyword } })
}

export function getQuoteOrder(quoteId) {
  return http.get(`/quote-details/quotes/${quoteId}`)
}

export function updateQuoteOrder(quoteId, payload) {
  return http.put(`/quote-details/quotes/${quoteId}`, payload)
}

export function listQuoteDetails(quoteId) {
  return http.get('/quote-details', { params: { quoteId } })
}

export function listQuoteLogs(quoteId) {
  return http.get(`/quote-details/quotes/${quoteId}/logs`)
}

export function deleteQuoteOrder(quoteId) {
  return http.delete(`/quote-details/quotes/${quoteId}`)
}

export function createQuoteAssignment(payload) {
  return http.post('/quote-details/assignments', payload)
}

export function updateQuoteAssignment(id, payload) {
  return http.put(`/quote-details/assignments/${id}`, payload)
}

export function listQuoteAssignments(contractId, keyword) {
  return http.get('/quote-details/assignments', { params: { contractId, keyword } })
}

export function listProductionLines(enabled) {
  return http.get('/order-flow/production-lines', { params: { enabled } })
}

export function createProductionLineV3(payload) {
  return http.post('/order-flow/production-lines', payload)
}

export function updateProductionLineV3(id, payload) {
  return http.put(`/order-flow/production-lines/${id}`, payload)
}

export function listReviewPoolV3() {
  return http.get('/order-flow/review-pool')
}

export function claimCustomerOrderReview(id) {
  return http.post(`/order-flow/customer-orders/${id}/claim-review`)
}

export function addSplitDraft(id, payload) {
  return http.post(`/order-flow/customer-orders/${id}/split-drafts`, payload)
}

export function listSplitDrafts(id) {
  return http.get(`/order-flow/customer-orders/${id}/split-drafts`)
}

export function submitCustomerOrderSplit(id) {
  return http.post(`/order-flow/customer-orders/${id}/submit-split`)
}

export function listFactoryOrdersV3(customerOrderId, status) {
  return http.get('/order-flow/factory-orders', { params: { customerOrderId, status } })
}

export function assignFactoryOrderV3(id, payload) {
  return http.post(`/order-flow/factory-orders/${id}/assign`, payload)
}

export function claimFactoryOrderV3(id) {
  return http.post(`/order-flow/factory-orders/${id}/claim`)
}

export function saveFactoryOrderQuoteV3(id, payload) {
  return http.post(`/order-flow/factory-orders/${id}/quotes`, payload)
}

export function listFactoryOrderQuotesV3(id) {
  return http.get(`/order-flow/factory-orders/${id}/quotes`)
}

export function requestFactoryOrderDiscountV3(id, payload) {
  return http.post(`/order-flow/factory-orders/${id}/discount-requests`, payload)
}

export function approveFactoryOrderDiscountV3(id, payload) {
  return http.post(`/order-flow/discount-requests/${id}/approve`, payload)
}

export function saveFactoryOrderExternalQuoteV3(id, payload) {
  return http.post(`/order-flow/factory-orders/${id}/external-quotes`, payload)
}

export function listFactoryOrderExternalQuotesV3(id) {
  return http.get(`/order-flow/factory-orders/${id}/external-quotes`)
}

export function getFactoryOrderQuoteV3(quoteId) {
  return http.get(`/order-flow/quotes/${quoteId}`)
}

export function requestPriceAdjustmentV3(id, payload) {
  return http.post(`/order-flow/customer-orders/${id}/price-adjustments`, payload)
}

export function approvePriceAdjustmentV3(id, payload) {
  return http.post(`/order-flow/price-adjustments/${id}/approve`, payload)
}

export function confirmCustomerQuoteV3(id, payload) {
  return http.post(`/order-flow/customer-orders/${id}/quote-confirmations`, payload)
}

export function createPaymentPlanV3(id, payload) {
  return http.post(`/order-flow/customer-orders/${id}/payment-plans`, payload)
}

export function approvePaymentPlanV3(id, payload) {
  return http.post(`/order-flow/customer-orders/${id}/payment-plans/approve`, payload)
}

export function listPaymentRemindersV3() {
  return http.get('/order-flow/payment-reminders')
}

export function listCommercialOrdersV3() {
  return http.get('/order-flow/commercial-orders')
}

export function getCommercialDetailV3(id) {
  return http.get(`/order-flow/customer-orders/${id}/commercial`)
}

export function getOrderFlowWorkQueues() {
  return http.get('/order-flow/work-queues')
}

export function submitPaymentReceiptV3(id, payload) {
  return http.post(`/order-flow/customer-orders/${id}/receipts`, payload)
}

export function confirmPaymentReceiptV3(id, payload) {
  return http.post(`/order-flow/receipts/${id}/confirm`, payload)
}

export function requestCuttingReleaseV3(id, payload) {
  return http.post(`/order-flow/customer-orders/${id}/cutting-release`, payload)
}

export function financeConfirmCuttingReleaseV3(id, payload) {
  return http.post(`/order-flow/cutting-release/${id}/finance-confirm`, payload)
}

export function directorApproveCuttingReleaseV3(id, payload) {
  return http.post(`/order-flow/cutting-release/${id}/director-approve`, payload)
}

export function releaseToCuttingV3(id) {
  return http.post(`/order-flow/cutting-release/${id}/release`)
}

export function uploadCustomerOrderCad(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/customer-orders/${id}/cad-files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listCustomerOrderCad(id) {
  return http.get(`/customer-orders/${id}/cad-files`)
}

export function listReceivingAccountsV3(enabled) {
  return http.get('/order-flow/receiving-accounts', { params: { enabled } })
}

export function createReceivingAccountV3(payload) {
  return http.post('/order-flow/receiving-accounts', payload)
}

export function updateReceivingAccountV3(id, payload) {
  return http.put(`/order-flow/receiving-accounts/${id}`, payload)
}

export function listHardwareItemsV3(keyword) {
  return http.get('/order-flow/hardware-items', { params: { keyword } })
}

export function createHardwareItemV3(payload) {
  return http.post('/order-flow/hardware-items', payload)
}

export function uploadQuoteAttachmentV3(file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/order-flow/quote-attachments', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export function getCompanyProfileV3() {
  return http.get('/order-flow/company-profile')
}

export function updateCompanyProfileV3(payload) {
  return http.put('/order-flow/company-profile', payload)
}

export function generateCustomerQuotePdfV3(id, payload) {
  return http.post(`/order-flow/customer-orders/${id}/quote-pdfs`, payload)
}

export function listCustomerQuotePdfsV3(id) {
  return http.get(`/order-flow/customer-orders/${id}/quote-pdfs`)
}

export function downloadCustomerQuotePdfV3(pdfId) {
  return http.get(`/order-flow/quote-pdfs/${pdfId}/download`, { responseType: 'blob' })
}
