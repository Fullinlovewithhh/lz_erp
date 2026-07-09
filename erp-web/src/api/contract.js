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

export function createCustomerOrder(payload) {
  return http.post('/customer-orders', payload)
}

export function listCustomerOrders(projectId, customerId, keyword) {
  return http.get('/customer-orders', { params: { projectId, customerId, keyword } })
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
