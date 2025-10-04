import request from '@/utils/request'

// 查询优惠券列表
export function listVoucher(query) {
  return request({
    url: '/marketing/voucher/list',
    method: 'get',
    params: query
  })
}

// 查询优惠券详细
export function getVoucher(id) {
  return request({
    url: '/marketing/voucher/' + id,
    method: 'get'
  })
}

// 新增优惠券
export function addVoucher(data) {
  return request({
    url: '/marketing/voucher',
    method: 'post',
    data: data
  })
}

// 修改优惠券
export function updateVoucher(data) {
  return request({
    url: '/marketing/voucher',
    method: 'put',
    data: data
  })
}

// 删除优惠券
export function delVoucher(id) {
  return request({
    url: '/marketing/voucher/' + id,
    method: 'delete'
  })
}
