package com.shaishavgandhi.mapper.sample

import com.shaishavgandhi.mapper.Mapper

@Mapper(to = DbUser::class)
data class APIUser(
  val name: String,
  val whatever: Int
)
