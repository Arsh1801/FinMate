import axios from 'axios';

const axiosClient = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL || ''}/api/v1`,
  headers: {
    'Content-Type': 'application/json'
  }
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request interceptor
axiosClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('finmate_access_token');
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// Response interceptor
axiosClient.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    // Check if error is 401 and not already retried
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.includes('/auth/login') && !originalRequest.url.includes('/auth/refresh')) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return axiosClient(originalRequest);
          })
          .catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('finmate_refresh_token');
      if (!refreshToken) {
        isRefreshing = false;
        localStorage.removeItem('finmate_access_token');
        localStorage.removeItem('finmate_user');
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const resp = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL || ''}/api/v1/auth/refresh`,
          { refreshToken }
        );
        const { accessToken, refreshToken: newRefresh } = resp.data;

        localStorage.setItem('finmate_access_token', accessToken);
        if (newRefresh) {
          localStorage.setItem('finmate_refresh_token', newRefresh);
        }

        axiosClient.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
        processQueue(null, accessToken);

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return axiosClient(originalRequest);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        localStorage.removeItem('finmate_access_token');
        localStorage.removeItem('finmate_refresh_token');
        localStorage.removeItem('finmate_user');
        window.location.href = '/login';
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
