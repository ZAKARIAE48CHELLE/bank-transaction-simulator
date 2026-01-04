-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jan 04, 2026 at 11:50 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `bank_simulator`
--

-- --------------------------------------------------------

--
-- Table structure for table `accounts`
--

CREATE TABLE `accounts` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `balance` double NOT NULL,
  `account_ref` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `accounts`
--

INSERT INTO `accounts` (`id`, `user_id`, `balance`, `account_ref`) VALUES
(1, 1, 43767, 'ACC-1'),
(2, 1, 1296, 'ACC-2'),
(3, 2, 6904, 'ACC-3');

-- --------------------------------------------------------

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `id` int(11) NOT NULL,
  `type` enum('DEPOSIT','WITHDRAW','TRANSFER') NOT NULL,
  `from_account_ref` varchar(50) DEFAULT NULL,
  `to_account_ref` varchar(50) DEFAULT NULL,
  `amount` double NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` enum('PENDING','DONE','FAILED') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `transactions`
--

INSERT INTO `transactions` (`id`, `type`, `from_account_ref`, `to_account_ref`, `amount`, `created_at`, `status`) VALUES
(1, 'DEPOSIT', NULL, 'ACC-1', 1200, '2026-01-03 16:14:54', 'PENDING'),
(2, 'TRANSFER', 'ACC-1', 'ACC-3', 3000, '2026-01-03 16:31:16', 'PENDING'),
(3, 'DEPOSIT', NULL, 'ACC-1', 12223, '2026-01-03 16:38:50', 'PENDING'),
(4, 'TRANSFER', 'ACC-1', 'ACC-2', 1200, '2026-01-03 23:49:50', 'DONE'),
(5, 'TRANSFER', 'ACC-1', 'ACC-3', 158, '2026-01-04 00:02:46', 'DONE'),
(6, 'TRANSFER', 'ACC-1', 'ACC-3', 162, '2026-01-04 00:02:46', 'DONE'),
(7, 'TRANSFER', 'ACC-1', 'ACC-3', 123, '2026-01-04 00:02:46', 'DONE'),
(8, 'TRANSFER', 'ACC-1', 'ACC-2', 144, '2026-01-04 00:02:46', 'DONE'),
(9, 'TRANSFER', 'ACC-2', 'ACC-1', 77, '2026-01-04 00:02:46', 'DONE'),
(10, 'TRANSFER', 'ACC-2', 'ACC-3', 126, '2026-01-04 00:02:46', 'DONE'),
(11, 'TRANSFER', 'ACC-3', 'ACC-1', 49, '2026-01-04 00:02:46', 'DONE'),
(12, 'TRANSFER', 'ACC-3', 'ACC-2', 106, '2026-01-04 00:02:46', 'DONE'),
(13, 'TRANSFER', 'ACC-1', 'ACC-2', 75, '2026-01-04 00:02:46', 'DONE'),
(14, 'TRANSFER', 'ACC-3', 'ACC-2', 187, '2026-01-04 00:02:46', 'DONE'),
(15, 'TRANSFER', 'ACC-2', 'ACC-3', 96, '2026-01-04 00:02:46', 'DONE'),
(16, 'TRANSFER', 'ACC-2', 'ACC-3', 170, '2026-01-04 00:02:46', 'DONE'),
(17, 'TRANSFER', 'ACC-2', 'ACC-3', 112, '2026-01-04 00:02:46', 'DONE'),
(18, 'TRANSFER', 'ACC-2', 'ACC-1', 52, '2026-01-04 00:02:46', 'DONE'),
(19, 'TRANSFER', 'ACC-1', 'ACC-2', 24, '2026-01-04 00:02:46', 'DONE'),
(20, 'TRANSFER', 'ACC-3', 'ACC-1', 43, '2026-01-04 00:02:46', 'DONE'),
(21, 'TRANSFER', 'ACC-3', 'ACC-2', 109, '2026-01-04 00:02:46', 'DONE'),
(22, 'TRANSFER', 'ACC-2', 'ACC-3', 144, '2026-01-04 00:02:46', 'DONE'),
(23, 'TRANSFER', 'ACC-1', 'ACC-3', 119, '2026-01-04 00:02:46', 'DONE'),
(24, 'TRANSFER', 'ACC-1', 'ACC-3', 140, '2026-01-04 00:02:46', 'DONE'),
(25, 'TRANSFER', 'ACC-2', 'ACC-1', 196, '2026-01-04 00:02:46', 'DONE'),
(26, 'TRANSFER', 'ACC-2', 'ACC-3', 140, '2026-01-04 00:02:46', 'DONE'),
(27, 'TRANSFER', 'ACC-1', 'ACC-2', 10, '2026-01-04 00:02:46', 'DONE'),
(28, 'TRANSFER', 'ACC-1', 'ACC-3', 51, '2026-01-04 00:02:46', 'DONE'),
(29, 'TRANSFER', 'ACC-1', 'ACC-3', 18, '2026-01-04 00:02:46', 'DONE'),
(30, 'TRANSFER', 'ACC-3', 'ACC-2', 87, '2026-01-04 00:02:46', 'DONE'),
(31, 'TRANSFER', 'ACC-3', 'ACC-1', 22, '2026-01-04 00:02:46', 'DONE'),
(32, 'TRANSFER', 'ACC-1', 'ACC-2', 21, '2026-01-04 00:02:46', 'DONE'),
(33, 'TRANSFER', 'ACC-2', 'ACC-1', 55, '2026-01-04 00:02:46', 'DONE'),
(34, 'TRANSFER', 'ACC-2', 'ACC-3', 168, '2026-01-04 00:02:46', 'DONE'),
(35, 'TRANSFER', 'ACC-2', 'ACC-1', 112, '2026-01-04 00:02:46', 'DONE'),
(36, 'TRANSFER', 'ACC-1', 'ACC-3', 126, '2026-01-04 00:02:46', 'DONE'),
(37, 'TRANSFER', 'ACC-3', 'ACC-1', 57, '2026-01-04 00:02:46', 'DONE'),
(38, 'TRANSFER', 'ACC-1', 'ACC-2', 131, '2026-01-04 00:02:46', 'DONE'),
(39, 'TRANSFER', 'ACC-1', 'ACC-2', 114, '2026-01-04 00:02:46', 'DONE'),
(40, 'TRANSFER', 'ACC-2', 'ACC-1', 190, '2026-01-04 00:02:46', 'DONE'),
(41, 'TRANSFER', 'ACC-2', 'ACC-1', 81, '2026-01-04 00:02:46', 'DONE'),
(42, 'TRANSFER', 'ACC-2', 'ACC-1', 93, '2026-01-04 00:02:46', 'DONE'),
(43, 'TRANSFER', 'ACC-1', 'ACC-3', 110, '2026-01-04 00:02:46', 'DONE'),
(44, 'TRANSFER', 'ACC-1', 'ACC-3', 147, '2026-01-04 00:02:46', 'DONE'),
(45, 'TRANSFER', 'ACC-2', 'ACC-1', 72, '2026-01-04 00:02:46', 'DONE'),
(46, 'TRANSFER', 'ACC-2', 'ACC-1', 24, '2026-01-04 00:02:46', 'DONE'),
(47, 'TRANSFER', 'ACC-1', 'ACC-3', 169, '2026-01-04 00:02:46', 'DONE'),
(48, 'TRANSFER', 'ACC-2', 'ACC-3', 86, '2026-01-04 00:02:46', 'DONE'),
(49, 'TRANSFER', 'ACC-3', 'ACC-1', 14, '2026-01-04 00:02:46', 'DONE'),
(50, 'TRANSFER', 'ACC-2', 'ACC-1', 186, '2026-01-04 00:02:46', 'DONE'),
(51, 'TRANSFER', 'ACC-1', 'ACC-3', 175, '2026-01-04 00:02:46', 'DONE'),
(52, 'TRANSFER', 'ACC-1', 'ACC-2', 179, '2026-01-04 00:02:46', 'DONE'),
(53, 'TRANSFER', 'ACC-1', 'ACC-2', 127, '2026-01-04 00:02:46', 'DONE'),
(54, 'TRANSFER', 'ACC-2', 'ACC-3', 38, '2026-01-04 00:02:46', 'DONE');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('CLIENT','ADMIN') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password`, `role`) VALUES
(1, 'client1', '1234', 'CLIENT'),
(2, 'client2', '1234', 'CLIENT'),
(3, 'admin1', 'admin', 'ADMIN');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `accounts`
--
ALTER TABLE `accounts`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `account_ref` (`account_ref`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `from_account_ref` (`from_account_ref`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `accounts`
--
ALTER TABLE `accounts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=55;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `accounts`
--
ALTER TABLE `accounts`
  ADD CONSTRAINT `accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `transactions`
--
ALTER TABLE `transactions`
  ADD CONSTRAINT `transactions_ibfk_1` FOREIGN KEY (`from_account_ref`) REFERENCES `accounts` (`account_ref`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
