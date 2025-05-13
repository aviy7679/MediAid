import React from 'react'
import DiseaseSearch from './DiseaseSearch'

export default function FillUserData() {
  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <h1 className="text-2xl font-bold mb-4">Disease Lookup</h1>
      <DiseaseSearch />
    </div>
  )
}
