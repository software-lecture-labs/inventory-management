import React from "react";
import { Navigate } from "react-router-dom";

const PrivateRoute = ({ children }) => {
  const user = JSON.parse(localStorage.getItem("user"));

  if (!user || !user.fullName) {
    // login sayfasına yönlendirmeden önce uyarı için flag ayarla
    localStorage.setItem("authMessage", "Önce giriş yapmalısınız!");
    return <Navigate to="/giris" replace />;
  }

  return children;
};

export default PrivateRoute;
