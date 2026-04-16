/**
 * 通用类型定义
 */

// 通用分页参数
export interface PageParams {
  page: number
  size: number
  keyword?: string
}

// 分页结果
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

// 通用响应结构
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// 用户相关
export interface User {
  id: number
  username: string
  nickname?: string
  email: string
  phone?: string
  avatar?: string
  role: string
  createTime?: string
}

// 房源相关
export interface Property {
  id: number
  title: string
  address: string
  price: number
  area: number
  rooms: number
  livingRooms: number
  bathrooms: number
  floor: number
  totalFloors: number
  type: string
  decoration: string
  description?: string
  images?: string[]
  status: number
  createTime?: string
}

export interface Viewing {
  id: number
  propertyId: number
  userId: number
  viewingTime: string
  contactPhone: string
  status: number
  remark?: string
  createTime?: string
}

// 服务订单相关
export interface ServiceType {
  id: number
  name: string
  description?: string
  price: number
  unit: string
  icon?: string
  sortOrder?: number
  status: number
}

export interface ServiceStaff {
  id: number
  userId: number
  name: string
  phone?: string
  avatar?: string
  storeId?: number
  serviceTypeId: number
  status: number
  starRating?: number
  orderCount?: number
}

export interface ServiceOrder {
  id: number
  orderNo: string
  userId: number
  storeId?: number
  serviceTypeId: number
  staffId?: number
  propertyId?: number
  serviceAddress: string
  serviceTime: string
  duration?: number
  totalAmount: number
  status: number
  payStatus: number
  payMethod?: string
  payTime?: string
  remark?: string
  cancelReason?: string
  createTime?: string
  // 关联数据
  serviceTypeName?: string
  staffName?: string
}

export interface Review {
  id: number
  orderId: number
  userId: number
  staffId: number
  storeId?: number
  rating: number
  content?: string
  images?: string[]
  isAnonymous: boolean
  createTime?: string
}

// 资产相关
export interface ProcurementProduct {
  id: number
  name: string
  description?: string
  category: string
  price: number
  stock: number
  unit?: string
  image?: string
  images?: string[]
  storeId?: number
  status: number
  createTime?: string
}

export interface SecondhandItem {
  id: number
  userId: number
  storeId?: number
  title: string
  description?: string
  category: string
  price: number
  condition: string
  image?: string
  images?: string[]
  status: number
  viewCount: number
  expireTime?: string
  createTime?: string
}

// AI 聊天相关
export interface ChatMessage {
  id?: string
  role: 'user' | 'assistant'
  content: string
  timestamp?: number
  agent?: string
}

export interface ChatSession {
  id: string
  title?: string
  messages: ChatMessage[]
  lastMessage?: string
  lastTime?: string
}
