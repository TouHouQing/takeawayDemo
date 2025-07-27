// 修复 vue-router 类型导入问题
declare module 'vue-router' {
  export interface Route {
    path: string
    name?: string
    hash: string
    query: { [key: string]: string | (string | null)[] | null | undefined }
    params: { [key: string]: string }
    fullPath: string
    matched: RouteRecord[]
    meta?: any
    redirectedFrom?: string
  }

  export interface RouteRecord {
    path: string
    regex?: RegExp
    components?: { [name: string]: any }
    instances?: { [name: string]: any }
    name?: string
    parent?: RouteRecord
    redirect?: any
    matchAs?: string
    meta?: any
    beforeEnter?: any
    props?: any
    children?: RouteRecord[]
  }

  export interface RouteConfig {
    path: string
    component?: any
    name?: string
    components?: { [name: string]: any }
    redirect?: any
    props?: any
    alias?: string | string[]
    children?: RouteConfig[]
    beforeEnter?: any
    meta?: any
    caseSensitive?: boolean
    pathToRegexpOptions?: any
  }
}
