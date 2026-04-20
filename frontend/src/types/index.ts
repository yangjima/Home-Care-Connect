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
  list?: T[]
  records?: T[]
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
  realName?: string
  /** male | female */
  gender?: string
  email: string
  phone?: string
  avatar?: string
  role: string
  /** 所属门店（店长/服务人员等） */
  storeId?: number | null
  storeName?: string | null
  /** 账号状态：active / inactive / banned */
  status?: string
  createTime?: string
  firstLoginAt?: string
  lastLoginAt?: string
}

/** GET /user/users/stats */
export interface UserStats {
  total: number
  serviceStaff: number
  distributor: number
  online: number
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
  videos?: string[]
  /** 列表封面，需在 images 中 */
  coverImage?: string
  status: number | string
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
  /** cleaning | repair | other */
  category?: string
  price: number
  unit: string
  icon?: string
  sortOrder?: number
  status: number
}

/** 服务列表页公开服务人员（/api/service/staff） */
export interface ServiceStaffPublic {
  id: number
  name: string
  skillsLabel: string
  rating: number
  completedOrders: number
  avatar?: string
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
  status: number | string
  payStatus: number | string | null
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
  images?: string[] | string
  salesCount?: number
  productTag?: string | null
  storeId?: number
  status?: number | string
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
  originalPrice?: number
  condition: string
  image?: string
  images?: string[]
  status: number | string
  viewCount?: number
  expireTime?: string
  createTime?: string
  /** 卖家展示名（后端聚合） */
  userName?: string
  integrityTag?: boolean
  location?: string
}

// AI 聊天相关
export interface ChatMessage {
  id?: string
  role: 'user' | 'assistant'
  content: string
  timestamp?: number
  agent?: string
  redirect?: string | null
  filters?: Record<string, unknown>
}

export interface ChatSession {
  id: string
  title?: string
  messages: ChatMessage[]
  lastMessage?: string
  lastTime?: string
}
