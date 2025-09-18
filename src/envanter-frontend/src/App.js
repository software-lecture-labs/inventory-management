import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Giris from './giris';
import Ekipman from './ekipman';
import Rezervasyon from './rezervasyon';
import Register from './register';
import Rapor from './rapor';
import RezervasyonTakvimi from './takvim';
import PrivateRoute from "./PrivateRoute";

function App() {
  return (
    <Router>
      <Routes>
        {/* Login ve Register serbest */}
        <Route path="/" element={<Giris />} />
        <Route path="/giris" element={<Giris />} />
        <Route path="/register" element={<Register />} />

        {/* Korumalı sayfalar */}
        <Route path="/ekipman" element={
          <PrivateRoute>
            <Ekipman />
          </PrivateRoute>
        } />
        <Route path="/rezervasyon" element={
          <PrivateRoute>
            <Rezervasyon />
          </PrivateRoute>
        } />
        <Route path="/rapor" element={
          <PrivateRoute>
            <Rapor />
          </PrivateRoute>
        } />
        <Route path="/deneme" element={
          <PrivateRoute>
            <RezervasyonTakvimi />
          </PrivateRoute>
        } />

        {/* Tanımsız route → login'e yönlendir */}
        <Route path="*" element={<Navigate to="/giris" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
