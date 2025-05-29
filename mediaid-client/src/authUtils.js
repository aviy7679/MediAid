import axios from "axios";

export const auth={
    setToken: (token) => localStorage.setItem("mediaid_token", token),
    getToken: () => localStorage.getItem("mediaid_token"),
    removeToken: () => {
        localStorage.removeItem("mediaid_token");
        localStorage.removeItem("mediaid_user");
    },

    setUser: (user) => localStorage.setItem("mediaid_user", JSON.stringify(user)),
    getUser: () => {
        const user = localStorage.getItem("mediaid_user");
        return user ? JSON.parse(user) : null;
    },
    getUserId: () => {
        const token = auth.getToken();
        if (!token) return null;

        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.userId; // UUID
        } catch {
            return null;
        }
    },

    isAuthenticated: () => {
        const token = auth.getToken();
        if (!token) return false;

        try{
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.exp > Date.now() / 1000;
        }catch{
            return false;
        }
    },

    logout: () => {
        auth.removeToken();
        window.location.href = "/";
    }
};


axios.interceptors.request.use(
    Response => Response,
    error=> {
        if(error.response?.status === 401) {
            auth.logout();
        }
        return Promise.reject(error);
    }
)