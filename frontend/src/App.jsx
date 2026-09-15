import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import Navbar from './components/Navbar';
import AdminNavbar from './components/AdminNavbar';
import Sidebar from './components/Sidebar';
import Home from './pages/home/Home';
import Login from './pages/login/Login';
import Business from './pages/business/Business';
import Data from './pages/data/Data';
import Report from './pages/report/Report';
import ErrorPage from './pages/error/ErrorPage';
import Admin from './pages/admin/Admin';
import AdminRoute from './components/AdminRoute';

import './App.css';

const MainLayout = () => {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="app-content">
        <Outlet />
      </main>
      <Sidebar />
    </div>
  );
};

const AdminLayout = () => {
  return (
    <div className="app-shell">
      <AdminNavbar />
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  );
};

function App() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminRoute><Admin /></AdminRoute>} />

          <Route path="error/:code" element={<ErrorPage />} />
          <Route path="*" element={<Navigate to="/admin/error/404" replace />} />
        </Route>

        <Route element={<MainLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/business" element={<Business />} />
          <Route path="/data" element={<Data />} />
          <Route path="/report" element={<Report />} />

          <Route path="/error/:code" element={<ErrorPage />} />
          <Route path="*" element={<Navigate to="/error/404" replace />} />
        </Route>

      </Routes>

      <ToastContainer position="top-center" autoClose={3000} hideProgressBar={true} />
    </BrowserRouter>
  );
}

export default App;