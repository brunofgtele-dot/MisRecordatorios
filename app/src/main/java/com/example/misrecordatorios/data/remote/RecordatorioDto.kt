
package com.example.misrecordatorios.data.remote

import com.google.gson.annotations.SerializedName

data class RecordatorioDto(

    @SerializedName("uuid")
    val uuid: String?,

    @SerializedName("titulo")
    val titulo: String?,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("categoria")
    val categoria: String?,

    @SerializedName("prioridad")
    val prioridad: String?,

    @SerializedName("estado")
    val estado: String?,

    @SerializedName("ubicacion")
    val ubicacion: String? = null,

    @SerializedName("fechaAviso")
    val fechaAviso: String? = null,

    @SerializedName("fechaCreacion")
    val fechaCreacion: Long?,

    @SerializedName("fechaModificacion")
    val fechaModificacion: Long?,

    @SerializedName("revision")
    val revision: Long?,

    @SerializedName("eliminado")
    val eliminado: Boolean?
)
 