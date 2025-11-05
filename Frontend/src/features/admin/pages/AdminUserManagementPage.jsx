import React from 'react'
import { useEffect, useState } from 'react';
import { getAllUsers, updateUser, createUser, deleteUser, patchUser } from '../services';
import { toast } from 'react-toastify';

import { 
  Plus, 
  Edit2, 
  Trash2, 
  X, 
  Save, 
  Search,
  Building2,
  Mail,
  Phone,
  MapPin,
  Globe,
  Image as ImageIcon
} from 'lucide-react';

const AdminUserManagementPage = () => {
  const [users, setUsers] = useState([]);
  const [isLoading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    avatarUrl: '',
    phone: '',
    role: 'CUSTOMER',
    status: 'ACTIVE',
    address: ''
  });
  const [errors, setErrors] = useState({});
    
  useEffect(() => {
    const fetchUsers = async () => {
      try {
      const authUsers = await getAllUsers();
      console.log("📦 getAllUsers() returned:", authUsers);
      setUsers(authUsers);
    } catch (error){
      console.error("Problem fetching users: ", error);
      toast.error('Failed to load users. Please try again.');
    } finally {
      setLoading(false);
    }
    }
    fetchUsers();
  }, [])

  const filteredUsers = users.filter(
    (user) =>
      user.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.phone.includes(searchQuery),
  )

  const handleEdit = (user) => {
    setEditingUser(user)
    setFormData({
      fullName: user.fullName,
      email: user.email,
      phone: user.phone,
      address: user.address,
      avatarUrl: user.avatarUrl || "",
      role: user.role?.toUpperCase(),
      status: user.status?.toUpperCase(),
    })
    setShowModal(true)
  }

  const handleDelete = (id) => {
    if (confirm("Are you sure you want to delete this user?")) {
      deleteUser(id)
    }
  }

  const handleToggleStatus = (id) => {
    const user = users.find(user => user.id === id);
    if (user){
      setUsers(users.map((user) =>
      user.id === id
      ? {
          ...user,
          status: user.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE",
        }
      : user
      ));

      updateUser(user.id, {
            status: user.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE",
          })
    }
    
  };

  const handleSubmit = (e) => {
    e.preventDefault()

    if (editingUser) {

    const updatedUser = {
      ...editingUser,
      fullName: formData.fullName ?? editingUser.fullName,
      email: formData.email ?? editingUser.email,
      phone: formData.phone ?? editingUser.phone,
      role: formData.role ?? editingUser.role,
      status: formData.status ?? editingUser.status,
      avatarUrl: formData.avatarUrl ?? editingUser.avatarUrl,
    };

    setUsers(users.map((user) => (user.id === editingUser.id ? updatedUser : user)));

    updateUser(editingUser.id, {
      fullName: formData.fullName,
      email: formData.email,
      phone: formData.phone,
      role: formData.role,
      status: formData.status,
      avatarUrl: formData.avatarUrl,
    });
  }

    setShowModal(false);
  }


  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
                <div className="mb-8">
                  <div className="flex items-center justify-between">
                    <div>
                      <h1 className="text-3xl font-bold text-gray-900 mb-2">User Management Console</h1>
                      <p className="text-gray-600">Moderate registered users and staffs</p>
                    </div>
                    <button
                      onClick={() => handleOpenModal()}
                      className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                      <Plus className="w-5 h-5" />
                      Create User
                    </button>
                  </div>
                </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="p-6">
          <p className="text-sm font-medium text-gray-600">Total Users</p>
          <p className="text-3xl font-bold text-gray-900 mt-2">{users.length}</p>
        </div>
        <div className="p-6">
          <p className="text-sm font-medium text-gray-600">Active Users</p>
          <p className="text-3xl font-bold text-green-600 mt-2">{users.filter((u) => u.status === "ACTIVE").length}</p>
        </div>
        <div className="p-6">
          <p className="text-sm font-medium text-gray-600">Banned Users</p>
          <p className="text-3xl font-bold text-red-600 mt-2">{users.filter((u) => u.status === "SUSPENDED").length}</p>
        </div>
        </div>
        
        {/* Search Bar */}
                <div className="mb-6">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                    <input
                      type="text"
                      placeholder="Search users by username, email, fullname..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>

        {/* Users Table */}
                {isLoading && users.length === 0 ? (
                  <div className="text-center py-12">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                    <p className="mt-4 text-gray-600">Loading users...</p>
                  </div>
                ) : filteredUsers.length === 0 ? (
                  <div className="text-center py-12 bg-white rounded-lg shadow">
                    <Building2 className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-600 text-lg">
                      {searchQuery ? 'No users found matching your search.' : 'No users found... (Unless network issues or not yet configured, this would not be possible)'}
                    </p>
                  </div>
                ) : (
                  <div className="bg-white rounded-lg shadow overflow-hidden">
                    <div className="overflow-x-auto">
                      <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                          <tr>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">User</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Contact</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Role</th>
                            <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Status</th>
                            <th className="px-6 py-4 text-right text-sm font-semibold text-gray-900">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                          {filteredUsers.map((user) => (
                            <tr key={user.userId} className="hover:bg-gray-50">
                              <td className="px-6 py-4 whitespace-nowrap">
                                <div className="flex items-center gap-3">
                                  <div className="w-10 h-10 rounded-full bg-[#ff6600] flex items-center justify-center text-white font-semibold">
                                    {user.fullName.charAt(0)}
                                  </div>
                                <div>
                                  <p className="text-sm font-medium text-gray-900">{user.fullName}</p>
                                  <p className="text-xs text-gray-500">Joined {user.createdAt}</p>
                                </div>
                                </div>
                              </td>
                              <td className="px-6 py-4 whitespace-nowrap">
                                <p className="text-sm text-gray-900">{user.email}</p>
                                <p className="text-xs text-gray-500">{user.phone}</p>
                              </td>
                              <td className="px-6 py-4 whitespace-nowrap">
                                <span
                                  className={`inline-block px-2 py-1 text-xs font-medium rounded ${
                                  user.role === "ADMIN" ? "bg-purple-100 text-purple-800" : "bg-blue-100 text-blue-800"
                                  }`}
                                >
                                  {user.role}
                                </span>
                              </td>
                              <td className="px-6 py-4 whitespace-nowrap">
                                <span
                                  className={`inline-block px-2 py-1 text-xs font-medium rounded ${
                                  user.status === "ACTIVE" ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"
                                  }`}
                                >
                                  {user.status}
                                </span>
                              </td>
                              <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                <div className="flex items-center justify-end gap-2">
                                    <button
                                      onClick={() => handleOpenModal(user)}
                                      className="text-blue-600 hover:text-blue-900 p-1 rounded hover:bg-blue-50"
                                      title="Edit"
                                    >
                                      <Edit2 className="w-4 h-4" />
                                    </button>
                                    <button
                                      onClick={() => handleDelete(user.userId)}
                                      className="text-red-600 hover:text-red-900 p-1 rounded hover:bg-red-50"
                                      title="Delete"
                                    >
                                      <Trash2 className="w-4 h-4" />
                                    </button>
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

      </div>
    </div>
  )
}

export default AdminUserManagementPage
