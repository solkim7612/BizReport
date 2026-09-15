import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      const errorCode = data?.code;

      if (status === 500 || errorCode === 'INTERNAL_SERVER_ERROR') {
        if (window.location.pathname.startsWith('/admin')) {
          window.location.href = '/admin/error/500';
        } else {
          window.location.href = '/error/500';
        }
        return Promise.reject(error);
      }

      if (status === 401) {
        window.location.href = '/login';
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;