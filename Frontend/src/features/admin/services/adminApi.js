import api from '../../../shared/services/api';

/**
 * Get user's list with pagination
 * Backend: GET /api/users?page={page}&size={size}
 * 
 * @param {number} page - Page number (0-indexed, default: 0)
 * @param {number} size - Page size (default: 10)
 * @returns {Promise<Object>}
 * */
export const getAllUsers = async (page, size) => {
  const response = await api.get("/users/", {
    params: { page: page, size: size, sort: "createdAt,desc" },
    })
  return response.data
}

/** Get user by ID */
export const getUserById = async (id) => {
  const response = await api.get(`/users/${id}`)
  return response.data
}

/** Update full user (PUT) */
export const updateUser = async (id, data) => {
  const response = await api.put(`/users/${id}`, data)
  return response.data
}

/** Partially update user (PATCH) */
export const patchUser = async (id, updates) => {
  const response = await api.patch(`/users/${id}`, updates)
  return response.data
}

/** Delete a user (requires ADMIN) */
export const deleteUser = async (id) => {
  const response = await api.delete(`/users/${id}`)
  return response.data
}

export default {
  getAllUsers,
  getUserById,
  updateUser,
  patchUser,
  deleteUser,
}