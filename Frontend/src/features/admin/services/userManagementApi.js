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
  try {
  const response = await api.get("/users/", {
    params: { page: page, size: size, sort: "createdAt,desc" },
    })
  return response.data.content || []
  } catch (error) {
    console.error("Problem with Get All: ", error)
    throw error
  }
}

/**
 * Fetch a user by their unique ID.
 *
 * @async
 * @function getUserById
 * @param {string|number} id - The unique ID of the user to retrieve.
 * @returns {Promise<Object>} A Promise resolving to the user data returned from the API.
 */
export const getUserById = async (id) => {
  try {
  const response = await api.get(`/users/${id}`)
  return response.data
  } catch (error) {
    console.error("Problem with Get By Id: ", error)
    throw error
  }
}

/**
 * Create a new user in the system.
 *
 * @async
 * @function createUser
 * @param {Object} userData - The data of the user to be created.
 * @param {string} userData.username - The username for the new user.
 * @param {string} userData.password - The password for the new user.
 * @param {string} userData.fullName - The full name of the new user.
 * @param {string} userData.email - The email address of the new user.
 * @param {string} userData.role - The assigned role (e.g. "STAFF", "CUSTOMER" or "ADMIN").
 * @returns {Promise<Object>} A Promise resolving to the created user’s data.
 */
export const createUser = async (userData) => {
  try {
  const response = await api.post("users/create", {
      username: userData.username,
      password: userData.password,     
      fullName: userData.fullName,
      email: userData.email,
      role: userData.role
    }
  );
  return response.data
  } catch (error) {
    console.error("Problem with Create: ", error)
    throw error
  }
}

/**
 * Replace an existing user's data with new values (HTTP PUT).
 *
 * @async
 * @function updateUser
 * @param {string|number} id - The unique ID of the user to update.
 * @param {Object} data - The complete user data to replace the existing entry.
 * @returns {Promise<Object>} A Promise resolving to the updated user data.
 */
export const updateUser = async (id, data) => {
  try {
  const response = await api.put(`/users/${id}`, data)
  return response.data
  } catch (error) {
    console.error("Problem with Update: ", error)
    throw error
  }
}

/**
 * Partially update specific fields of a user (HTTP PATCH).
 *
 * @async
 * @function patchUser
 * @param {string|number} id - The unique ID of the user to patch.
 * @param {Object} updates - A subset of fields to update (e.g., { email: "new@example.com" }).
 * @returns {Promise<Object>} A Promise resolving to the updated user data.
 */
export const patchUser = async (id, updates) => {
  try {
    const response = await api.patch(`/users/${id}`, updates)
    return response.data
  } catch (error) {
    console.error("Problem with Patch: ", error)
    throw error
  }
  
}

/**
 * Delete a user by their unique ID (requires ADMIN privileges).
 *
 * @async
 * @function deleteUser
 * @param {string|number} id - The unique ID of the user to delete.
 * @returns {Promise<Object>} A Promise resolving to the server’s response data after deletion.
 */
export const deleteUser = async (id) => {
  try {
  const response = await api.delete(`/users/${id}`)
  return response.data
  } catch (error) {
    console.error("Problem with Delete: ", error)
    throw error
  }
}

export default {
  getAllUsers,
  getUserById,
  updateUser,
  patchUser,
  deleteUser,
}