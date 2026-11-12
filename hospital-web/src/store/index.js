import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const TOKEN_KEY = 'hospital_token'
const ROLE_KEY = 'hospital_role'
const USER_KEY = 'hospital_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export default new Vuex.Store({
  state: {
    token: localStorage.getItem(TOKEN_KEY) || '',
    role: localStorage.getItem(ROLE_KEY) || '',
    userInfo: readUser()
  },
  getters: {
    token: state => state.token,
    role: state => state.role,
    userInfo: state => state.userInfo,
    name: state => (state.userInfo && state.userInfo.name) || '',
    isLogin: state => !!state.token
  },
  mutations: {
    SET_LOGIN(state, payload) {
      state.token = payload.token
      state.role = payload.role
      state.userInfo = payload.userInfo
      localStorage.setItem(TOKEN_KEY, payload.token)
      localStorage.setItem(ROLE_KEY, payload.role)
      localStorage.setItem(USER_KEY, JSON.stringify(payload.userInfo || null))
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo || null))
    },
    CLEAR_LOGIN(state) {
      state.token = ''
      state.role = ''
      state.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ROLE_KEY)
      localStorage.removeItem(USER_KEY)
    }
  },
  actions: {
    setLogin({ commit }, payload) {
      commit('SET_LOGIN', payload)
    },
    setUserInfo({ commit }, userInfo) {
      commit('SET_USER_INFO', userInfo)
    },
    clearLogin({ commit }) {
      commit('CLEAR_LOGIN')
    }
  }
})
