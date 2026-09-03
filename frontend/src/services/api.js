const API_ROOT = '/api'

async function request(path, options = {}) {
  const response = await fetch(`${API_ROOT}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(body.message || body.reason || `请求失败（${response.status}）`)
  }
  return body
}

export function loadExample(name) {
  return request(`/examples/${name}`)
}

export function analyzeDataset(dataset, thresholds) {
  const params = new URLSearchParams({
    imbalanceRatioThreshold: String(thresholds.imbalanceRatioThreshold),
    giniThreshold: String(thresholds.giniThreshold),
    thresholdMode: thresholds.thresholdMode
  })
  return request(`/balance/analyze?${params.toString()}`, {
    method: 'POST',
    body: JSON.stringify(dataset)
  })
}

export function governDataset(payload) {
  return request('/balance/govern', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}
