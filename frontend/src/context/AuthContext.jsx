import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import client, { TOKEN_KEY } from "../api/client";
import { useToast } from "./ToastContext";

const AuthContext = createContext(null);

/**
 * Oturum bilgisini tutar. Token localStorage'da saklanır,
 * kullanıcı bilgisi (rol dahil) her açılışta backend'den tazelenir.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const { showToast } = useToast();

  // Sayfa açıldığında kayıtlı token varsa kullanıcıyı yükle
  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) {
      setLoading(false);
      return;
    }
    client
      .get("/auth/me")
      .then((res) => setUser(res.data))
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false));
  }, []);

  // Token süresi dolduğunda (api/client.js tetikler)
  useEffect(() => {
    const onExpired = () => {
      setUser(null);
      showToast("Oturumunuzun süresi doldu, lütfen tekrar giriş yapın", "warning");
    };
    window.addEventListener("auth:expired", onExpired);
    return () => window.removeEventListener("auth:expired", onExpired);
  }, [showToast]);

  const handleAuthResponse = (data) => {
    localStorage.setItem(TOKEN_KEY, data.token);
    setUser(data.user);
    return data.user;
  };

  const login = useCallback(async (email, password) => {
    const res = await client.post("/auth/login", { email, password });
    return handleAuthResponse(res.data);
  }, []);

  const register = useCallback(async (form) => {
    const res = await client.post("/auth/register", form);
    return handleAuthResponse(res.data);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setUser(null);
  }, []);

  // Şifre değiştirme gibi işlemlerden sonra güncel kullanıcı bilgisini yerleştirmek için
  const updateUser = useCallback((updated) => setUser(updated), []);

  const value = useMemo(
    () => ({ user, loading, isAdmin: user?.role === "ADMIN", login, register, logout, updateUser }),
    [user, loading, login, register, logout, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
