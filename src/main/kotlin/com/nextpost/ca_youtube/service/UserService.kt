package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.model.entity.Channel
import com.nextpost.ca_youtube.model.entity.User
import com.nextpost.ca_youtube.repository.UserRepository
import com.nextpost.ca_youtube.repository.ChannelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory

@Service
class UserService(
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    @Transactional
    fun getOrCreateUser(email: String, name: String, picture: String?): User {
        return userRepository.findByEmail(email) ?: userRepository.save(
            User(
                email = email,
                name = name,
                picture = picture
            )
        )
    }

    @Transactional
    fun addChannelToUser(userId: Long, channelId: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val channel = channelRepository.findByChannelId(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        user.channels.add(channel)
        userRepository.save(user)
    }

    @Transactional
    fun removeChannelFromUser(userId: Long, channelId: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val channel = channelRepository.findByChannelId(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        user.channels.remove(channel)
        userRepository.save(user)
    }

    fun getUserChannels(userId: Long): List<Channel> {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        return user.channels.toList()
    }
}
