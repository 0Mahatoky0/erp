import { api, ErreurApi } from '@/shared/api/axios'

export const paieApi = {
  variables: (debut, fin) => api.get(`/paie/variables?debut=${debut}&fin=${fin}`),
}

export { ErreurApi }
