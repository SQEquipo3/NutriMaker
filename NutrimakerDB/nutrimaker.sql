-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: localhost    Database: nutrimaker
-- ------------------------------------------------------
-- Server version	5.5.5-10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


CREATE DATABASE IF NOT EXISTS nutrimaker
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `nutrimaker`;



--
-- Table structure for table `comida`
--

DROP TABLE IF EXISTS `comida`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comida` (
  `ID_COMIDA` int(11) NOT NULL,
  `ID_DIETA` int(11) DEFAULT NULL,
  `DIA_SEMANA` tinyint(3) unsigned DEFAULT NULL,
  `HORA` time DEFAULT NULL,
  `TIPO_COMIDA` varchar(10) DEFAULT NULL,
  `GRUPO_COMIDA` varchar(15) DEFAULT NULL,
  `CALORIAS` decimal(9,4) DEFAULT NULL,
  `GRASAS` decimal(9,4) DEFAULT NULL,
  `COLESTEROL` decimal(9,4) DEFAULT NULL,
  `SODIO` decimal(9,4) DEFAULT NULL,
  `CARBOHIDRATOS` decimal(9,4) DEFAULT NULL,
  `PROTEINA` decimal(9,4) DEFAULT NULL,
  `CALCIO` decimal(9,4) DEFAULT NULL,
  `HIERRO` decimal(9,4) DEFAULT NULL,
  `NOMBRE_COMIDA` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`ID_COMIDA`),
  KEY `FK_COMIDA_DIETA` (`ID_DIETA`),
  CONSTRAINT `FK_COMIDA_DIETA` FOREIGN KEY (`ID_DIETA`) REFERENCES `dieta` (`ID_DIETA`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comida`
--

LOCK TABLES `comida` WRITE;
/*!40000 ALTER TABLE `comida` DISABLE KEYS */;
INSERT INTO `comida` VALUES (1,1,NULL,'08:00:00','Desayuno','Avena',508.7221,4.8682,0.0000,328.6004,94.9290,29.2089,65.7201,2.4341,'Egg White Oatmeal with Blueberries - Plan 01'),(2,1,NULL,'14:00:00','Cena','Ensalada',691.2779,38.9452,109.5335,408.9249,46.2475,46.2475,331.0345,4.8682,'Veggie & Chicken Salad - Plan 01'),(3,2,NULL,'08:00:00','Desayuno','Yogur',627.4563,13.1898,20.7268,173.3513,82.9071,52.7591,NULL,NULL,'Breakfast Parfait - Plan 02'),(4,2,NULL,'14:00:00','Cena','Ensalada',772.5437,58.4118,184.6568,446.5680,7.5370,56.5276,92.3284,1.8843,'Basic Chicken Salad - Plan 02'),(5,3,NULL,'08:00:00','Desayuno','Huevo y avena',994.5946,32.4324,467.0270,1234.5946,131.8919,54.0541,521.0811,6.4865,'Eggs and Oatmeal - Plan 03'),(6,3,NULL,'14:00:00','Cena','Ensalada',605.4054,30.2703,192.4324,614.0541,19.4595,60.5405,38.9189,2.1622,'Bistro Chicken Salad - Plan 03'),(7,4,NULL,'08:00:00','Desayuno','Avena',422.6966,4.0449,0.0000,273.0337,78.8764,24.2697,54.6067,2.0225,'Egg White Oatmeal with Blueberries - Plan 04'),(8,4,NULL,'13:00:00','Comida','Ensalada',829.2135,62.6966,198.2022,479.3258,8.0899,60.6742,99.1011,2.0225,'Basic Chicken Salad - Plan 04'),(9,4,NULL,'19:00:00','Cena','Ensalada',548.0899,30.3371,182.0225,768.5393,14.1573,60.6742,204.2697,2.0225,'Balsamic Parmesan Chicken Salad - Plan 04'),(10,5,NULL,'08:00:00','Desayuno','Yogur',742.4749,15.6076,24.5262,205.1282,98.1048,62.4303,NULL,NULL,'Breakfast Parfait - Plan 05'),(11,5,NULL,'13:00:00','Comida','Ensalada',633.2218,35.6745,100.3344,374.5819,42.3634,42.3634,303.2330,4.4593,'Veggie & Chicken Salad - Plan 05'),(12,5,NULL,'19:00:00','Cena','Ensalada',624.3032,31.2152,198.4392,633.2218,20.0669,62.4303,40.1338,2.2297,'Bistro Chicken Salad - Plan 05'),(13,6,NULL,'08:00:00','Desayuno','Huevo y avena',876.9497,28.5962,411.7851,1088.5615,116.2912,47.6603,459.4454,5.7192,'Eggs and Oatmeal - Plan 06'),(14,6,NULL,'13:00:00','Comida','Ensalada',781.6291,59.0988,186.8284,451.8198,7.6256,57.1924,93.4142,1.9064,'Basic Chicken Salad - Plan 06'),(15,6,NULL,'19:00:00','Cena','Ensalada',541.4211,30.5026,85.7886,320.2773,36.2218,36.2218,259.2721,3.8128,'Veggie & Chicken Salad - Plan 06'),(16,7,NULL,'08:00:00','Desayuno','Avena',430.1131,15.4162,6.1665,55.4985,55.4985,26.2076,178.8284,3.0832,'Peanut Butter Yogurt Oatmeal - Plan 07'),(17,7,NULL,'13:00:00','Comida','Ensalada',632.0658,47.7903,151.0791,365.3649,6.1665,46.2487,75.5396,1.5416,'Basic Chicken Salad - Plan 07'),(18,7,NULL,'19:00:00','Cena','Ensalada',437.8212,24.6660,69.3731,258.9928,29.2909,29.2909,209.6608,3.0832,'Veggie & Chicken Salad - Plan 07'),(19,8,NULL,'08:00:00','Desayuno','Yogur',394.6217,8.2954,13.0356,109.0246,52.1422,33.1814,NULL,NULL,'Breakfast Parfait - Plan 08'),(20,8,NULL,'10:30:00','Colacion','Avena',247.6755,2.3701,0.0000,159.9818,46.2170,14.2206,31.9964,1.1851,'Egg White Oatmeal with Blueberries - Plan 08'),(21,8,NULL,'14:00:00','Comida','Ensalada',336.5542,18.9608,53.3273,199.0884,22.5160,22.5160,161.1668,2.3701,'Veggie & Chicken Salad - Plan 08'),(22,8,NULL,'19:00:00','Cena','Ensalada',321.1486,17.7758,106.6545,450.3191,8.2954,35.5515,119.6901,1.1851,'Balsamic Parmesan Chicken Salad - Plan 08'),(23,9,NULL,'08:00:00','Desayuno','Huevo y avena',545.3278,17.7824,256.0669,676.9177,72.3152,29.6374,285.7043,3.5565,'Eggs and Oatmeal - Plan 09'),(24,9,NULL,'10:30:00','Colacion','Ensalada',331.9386,16.5969,105.5091,336.6806,10.6695,33.1939,21.3389,1.1855,'Bistro Chicken Salad - Plan 09'),(25,9,NULL,'14:00:00','Comida','Ensalada',336.6806,18.9679,53.3473,199.1632,22.5244,22.5244,161.2273,2.3710,'Veggie & Chicken Salad - Plan 09'),(26,9,NULL,'19:00:00','Cena','Ensalada',486.0530,36.7503,116.1785,280.9623,4.7420,35.5649,58.0893,1.1855,'Basic Chicken Salad - Plan 09'),(27,10,NULL,'08:00:00','Desayuno','Huevo y avena',648.3680,21.1424,304.4510,804.8220,85.9792,35.2374,339.6884,4.2285,'Eggs and Oatmeal - Plan 10'),(28,10,NULL,'10:30:00','Colacion','Yogur',469.3620,9.8665,15.5045,129.6736,62.0178,39.4659,NULL,NULL,'Breakfast Parfait - Plan 10'),(29,10,NULL,'14:00:00','Comida','Ensalada',381.9733,21.1424,126.8546,535.6083,9.8665,42.2849,142.3591,1.4095,'Balsamic Parmesan Chicken Salad - Plan 10'),(30,10,NULL,'19:00:00','Cena','Ensalada',400.2967,22.5519,63.4273,236.7953,26.7804,26.7804,191.6914,2.8190,'Veggie & Chicken Salad - Plan 10'),(31,11,NULL,'08:00:00','Desayuno','Huevo y avena',550.7412,17.9590,258.6089,683.6374,73.0331,29.9316,288.5405,3.5918,'Eggs and Oatmeal - Plan 11'),(32,11,NULL,'10:30:00','Colacion','Yogur',398.6887,8.3808,13.1699,110.1482,52.6796,33.5234,NULL,NULL,'Breakfast Parfait - Plan 11'),(33,11,NULL,'13:00:00','Comida','Ensalada',490.8780,37.1152,117.3318,283.7514,4.7891,35.9179,58.6659,1.1973,'Basic Chicken Salad - Plan 11'),(34,11,NULL,'16:30:00','Colacion','Ensalada',335.2338,16.7617,106.5564,340.0228,10.7754,33.5234,21.5507,1.1973,'Bistro Chicken Salad - Plan 11'),(35,11,NULL,'19:00:00','Cena','Ensalada',324.4584,17.9590,107.7537,454.9601,8.3808,35.9179,120.9236,1.1973,'Balsamic Parmesan Chicken Salad - Plan 11'),(36,12,NULL,'08:00:00','Desayuno','Huevo y avena',617.6299,20.1401,290.0175,766.6667,81.9031,33.5668,323.5844,4.0280,'Eggs and Oatmeal - Plan 12'),(37,12,NULL,'10:30:00','Colacion','Avena',374.6060,13.4267,5.3707,48.3363,48.3363,22.8255,155.7501,2.6853,'Peanut Butter Yogurt Oatmeal - Plan 12'),(38,12,NULL,'13:00:00','Comida','Ensalada',550.4962,41.6229,131.5820,318.2137,5.3707,40.2802,65.7910,1.3427,'Basic Chicken Salad - Plan 12'),(39,12,NULL,'16:30:00','Colacion','Ensalada',381.3193,21.4828,60.4203,225.5692,25.5108,25.5108,182.6036,2.6853,'Veggie & Chicken Salad - Plan 12'),(40,12,NULL,'19:00:00','Cena','Ensalada',375.9486,18.7974,119.4980,381.3193,12.0841,37.5949,24.1681,1.3427,'Bistro Chicken Salad - Plan 12'),(41,13,NULL,'08:00:00','Desayuno','Yogur',310.1796,6.5203,10.2462,85.6953,40.9847,26.0812,NULL,NULL,'Breakfast Parfait - Plan 13'),(42,13,NULL,'10:30:00','Colacion','Avena',194.6773,1.8629,0.0000,125.7485,36.3273,11.1776,25.1497,0.9315,'Egg White Oatmeal with Blueberries - Plan 13'),(43,13,NULL,'13:00:00','Comida','Ensalada',381.9029,28.8756,91.2841,220.7585,3.7259,27.9441,45.6420,0.9315,'Basic Chicken Salad - Plan 13'),(44,13,NULL,'16:30:00','Colacion','Ensalada',260.8117,13.0406,82.9009,264.5376,8.3832,26.0812,16.7665,0.9315,'Bistro Chicken Salad - Plan 13'),(45,13,NULL,'19:00:00','Cena','Ensalada',252.4285,13.9721,83.8323,353.9587,6.5203,27.9441,94.0785,0.9315,'Balsamic Parmesan Chicken Salad - Plan 13'),(46,14,NULL,'08:00:00','Desayuno','Huevo y avena',584.6467,19.0646,274.5297,725.7245,77.5292,31.7743,306.3040,3.8129,'Eggs and Oatmeal - Plan 14'),(47,14,NULL,'10:00:00','Colacion','Yogur',423.2333,8.8968,13.9807,116.9293,55.9227,35.5872,NULL,NULL,'Breakfast Parfait - Plan 14'),(48,14,NULL,'13:00:00','Comida','Avena',265.6329,2.5419,0.0000,171.5811,49.5679,15.2517,34.3162,1.2710,'Egg White Oatmeal with Blueberries - Plan 14'),(49,14,NULL,'16:00:00','Colacion','Ensalada',521.0981,39.4001,124.5552,301.2201,5.0839,38.1291,62.2776,1.2710,'Basic Chicken Salad - Plan 14'),(50,14,NULL,'19:00:00','Cena','Ensalada',360.9558,20.3355,57.1937,213.5231,24.1484,24.1484,172.8521,2.5419,'Veggie & Chicken Salad - Plan 14'),(51,14,NULL,'21:00:00','Colacion','Ensalada',344.4331,19.0646,114.3874,482.9690,8.8968,38.1291,128.3681,1.2710,'Balsamic Parmesan Chicken Salad - Plan 14'),(52,15,NULL,'08:00:00','Desayuno','Huevo y avena',434.1898,14.1584,203.8804,538.9617,57.5773,23.5973,227.4777,2.8317,'Eggs and Oatmeal - Plan 15'),(53,15,NULL,'10:00:00','Colacion','Avena',263.3456,9.4389,3.7756,33.9801,33.9801,16.0461,109.4913,1.8878,'Peanut Butter Yogurt Oatmeal - Plan 15'),(54,15,NULL,'08:00:00','Desayuno','Yogur',314.3157,6.6072,10.3828,86.8380,41.5312,26.4289,NULL,NULL,'Breakfast Parfait - Plan 15'),(55,15,NULL,'16:00:00','Colacion','Ensalada',264.2895,13.2145,84.0063,268.0650,8.4950,26.4289,16.9900,0.9439,'Bistro Chicken Salad - Plan 15'),(56,15,NULL,'19:00:00','Cena','Ensalada',268.0650,15.1023,42.4751,158.5737,17.9339,17.9339,128.3692,1.8878,'Veggie & Chicken Salad - Plan 15'),(57,15,NULL,'21:00:00','Colacion','Ensalada',255.7944,14.1584,84.9502,358.6786,6.6072,28.3167,95.3330,0.9439,'Balsamic Parmesan Chicken Salad - Plan 15'),(102,1,NULL,'08:00:00','Desayuno','Avena',239.6490,3.1994,4.6750,35.1475,38.1140,16.5495,143.8540,2.1981,'Blueberry Cinnamon Overnight Oats - NM Extra 01'),(103,1,NULL,'08:00:00','Desayuno','Yogur',236.5200,9.9800,8.5000,62.3800,17.7800,21.9900,251.4200,1.2360,'Strawberry Almond Yogurt Bowl - NM Extra 02'),(104,1,NULL,'08:00:00','Desayuno','Avena',361.7555,11.8738,0.0000,5.0370,54.9872,13.1502,42.4580,2.8647,'Apple Peanut Butter Oatmeal - NM Extra 03'),(105,1,NULL,'08:00:00','Desayuno','Huevo',221.0000,7.4360,0.0000,375.7000,11.3360,25.9480,31.9150,0.9490,'Egg White Pepper Scramble - NM Extra 04'),(106,1,NULL,'08:00:00','Desayuno','Huevo',186.2350,12.6735,316.2000,129.7100,6.3070,12.2145,72.7175,2.3120,'Egg and Tomato Breakfast Plate - NM Extra 05'),(107,1,NULL,'08:00:00','Desayuno','Yogur',295.9500,7.8350,8.0000,75.7500,37.5850,20.4350,197.4000,1.0550,'Granola Berry Parfait - NM Extra 06'),(108,1,NULL,'08:00:00','Desayuno','Avena',344.2985,15.8459,0.0000,2.4840,43.8368,9.9900,58.0060,2.8624,'Maple Pecan Oat Bowl - NM Extra 07'),(109,1,NULL,'08:00:00','Desayuno','Avena',417.0270,12.2694,4.8750,43.2120,56.9998,23.6379,208.1040,4.3251,'Strawberry Flax Oats - NM Extra 08'),(110,1,NULL,'08:00:00','Desayuno','Yogur',232.9340,5.0014,6.8000,60.6900,32.7760,16.4815,182.8690,0.7956,'Apple Cinnamon Yogurt Crunch - NM Extra 09'),(111,1,NULL,'08:00:00','Desayuno','Yogur',260.3900,9.9700,8.7500,66.9600,22.9500,23.1550,205.6400,0.7870,'Blueberry Peanut Yogurt - NM Extra 10'),(112,1,NULL,'08:00:00','Desayuno','Huevo',248.3080,10.5800,16.1920,630.7980,6.6700,30.5900,243.8920,0.7268,'Egg White Parmesan Scramble - NM Extra 11'),(113,1,NULL,'08:00:00','Desayuno','Avena',386.9190,13.4056,0.0000,2.7950,56.9972,14.3156,96.5640,3.8337,'Berry Almond Overnight Oats - NM Extra 12'),(114,1,NULL,'08:00:00','Desayuno','Yogur',220.0650,9.3738,6.8000,49.5210,22.0830,15.1215,159.6130,0.5083,'Cranberry Pecan Yogurt - NM Extra 13'),(115,1,NULL,'08:00:00','Desayuno','Fruta',289.8900,8.4720,0.0000,6.0000,47.6600,9.5000,71.9700,2.8240,'Apple Flax Breakfast Bowl - NM Extra 14'),(116,1,NULL,'08:00:00','Desayuno','Huevo',246.2150,13.4435,436.0800,471.0400,8.3950,22.4250,264.8450,2.6220,'Pepper Cheese Egg Scramble - NM Extra 15'),(117,1,NULL,'08:00:00','Desayuno','Avena',356.0310,4.6566,3.9000,32.6950,62.8342,18.6186,152.4380,3.3709,'Strawberry Maple Oats - NM Extra 16'),(118,1,NULL,'08:00:00','Desayuno','Avena',233.6650,5.8055,5.9500,56.3550,31.6965,15.3425,148.2400,0.8585,'Blueberry Granola Crunch - NM Extra 17'),(119,1,NULL,'08:00:00','Desayuno','Avena',328.3000,13.2000,0.0000,1.9800,44.8520,11.9260,101.8000,3.0440,'Almond Cinnamon Oatmeal - NM Extra 18'),(120,1,NULL,'08:00:00','Desayuno','Huevo',179.6300,3.3925,10.3500,682.2950,8.3547,29.2963,267.7775,0.7647,'Egg White Tomato Cheese Bowl - NM Extra 19'),(121,1,NULL,'08:00:00','Desayuno','Yogur',320.2940,11.5440,11.0500,84.3960,30.0560,28.3530,259.8440,0.7462,'Peanut Apple Yogurt Bowl - NM Extra 20'),(122,1,NULL,'14:00:00','Comida','Pollo',228.5310,10.8800,77.5625,59.4405,7.2420,25.7847,44.9140,1.6524,'Lemon Chicken Garden Salad - NM Extra 21'),(123,1,NULL,'14:00:00','Comida','Pollo',301.8200,11.9100,94.9000,132.0600,16.2000,31.2500,47.2800,1.3080,'Chicken Carrot Pepper Bowl - NM Extra 22'),(124,1,NULL,'14:00:00','Comida','Pollo',337.1340,16.8636,109.1350,293.8250,9.9682,37.2071,80.5920,2.8290,'Dijon Chicken Lettuce Salad - NM Extra 23'),(125,1,NULL,'14:00:00','Comida','Pollo',442.3900,22.2690,153.2830,524.2510,7.2280,51.7595,363.0900,1.4534,'Parmesan Chicken Tomato Plate - NM Extra 24'),(126,1,NULL,'14:00:00','Comida','Pollo',226.6100,6.1965,83.7675,257.6775,14.2630,27.9132,69.6235,1.8802,'Balsamic Chicken Romaine Bowl - NM Extra 25'),(127,1,NULL,'14:00:00','Comida','Fruta',304.7500,11.3100,91.2500,67.0000,20.0250,33.0300,92.2000,2.3550,'Chicken Apple Crunch Salad - NM Extra 26'),(128,1,NULL,'14:00:00','Comida','Pollo',343.0220,13.8690,121.7275,284.7860,12.6960,40.8997,62.2380,2.5610,'Pepper Chicken Dijon Salad - NM Extra 27'),(129,1,NULL,'14:00:00','Comida','Yogur',313.9500,5.7330,137.4100,223.8600,11.2710,52.7475,201.5000,2.3660,'Chicken Celery Yogurt Salad - NM Extra 28'),(130,1,NULL,'14:00:00','Comida','Fruta',291.6690,9.6118,83.7675,106.7175,25.1345,27.5715,60.7750,1.7085,'Cranberry Chicken Greens - NM Extra 29'),(131,1,NULL,'14:00:00','Comida','Pollo',310.4000,13.9600,102.2000,75.0000,12.1800,33.7600,35.9000,1.5200,'Chicken Shallot Tomato Bowl - NM Extra 30'),(132,1,NULL,'14:00:00','Comida','Pollo',356.9485,11.4655,140.9555,425.2240,13.0180,48.7428,321.4365,2.7278,'Parmesan Pepper Chicken Salad - NM Extra 31'),(133,1,NULL,'14:00:00','Comida','Frutos secos',395.6030,17.4850,123.3700,95.6670,15.5740,46.2930,142.3630,3.6309,'Chicken Almond Garden Bowl - NM Extra 32'),(134,1,NULL,'14:00:00','Comida','Pollo',261.9785,10.4116,86.8700,126.2080,13.4921,28.8643,70.2525,1.7935,'Lemon Chicken Carrot Salad - NM Extra 33'),(135,1,NULL,'14:00:00','Comida','Pollo',299.1800,13.9180,105.8500,328.3800,8.0660,35.0130,61.2000,1.4090,'Bistro Dijon Chicken Bowl - NM Extra 34'),(136,1,NULL,'14:00:00','Comida','Pollo',349.4620,10.8675,135.7460,400.6830,14.2830,46.6210,275.1030,1.7526,'Chicken Pepper Parmesan Plate - NM Extra 35'),(137,1,NULL,'14:00:00','Comida','Yogur',310.1150,5.7772,137.7350,131.0920,9.9580,53.4625,197.4700,3.2175,'Yogurt Herb Chicken Salad - NM Extra 36'),(138,1,NULL,'14:00:00','Comida','Fruta',238.1700,6.1030,86.8700,202.7250,17.4930,28.2795,46.2400,1.5980,'Balsamic Apple Chicken Bowl - NM Extra 37'),(139,1,NULL,'14:00:00','Comida','Pollo',310.9200,12.2500,105.8500,141.4100,15.0600,34.7850,54.3300,1.3480,'Tomato Carrot Chicken Plate - NM Extra 38'),(140,1,NULL,'14:00:00','Comida','Frutos secos',350.8880,15.0995,117.5300,182.1370,11.7645,42.6995,148.4880,2.6979,'Chicken Celery Almond Crunch - NM Extra 39'),(141,1,NULL,'14:00:00','Comida','Pollo',451.7240,21.5436,162.7730,536.0030,8.1562,55.3917,405.1450,2.7404,'Chicken Parmesan Romaine Salad - NM Extra 40'),(142,1,NULL,'20:00:00','Cena','Pollo',186.3200,7.1740,68.2550,54.2300,7.9560,23.0435,43.9875,1.5725,'Light Lemon Chicken Supper - NM Extra 41'),(143,1,NULL,'20:00:00','Cena','Pollo',244.3100,8.3730,83.9500,366.7500,13.4800,28.0450,57.6300,1.0670,'Warm Chicken Carrot Plate - NM Extra 42'),(144,1,NULL,'20:00:00','Cena','Pollo',263.9825,7.0553,96.5425,231.6675,16.0655,32.4933,53.1300,2.1677,'Balsamic Pepper Chicken Dinner - NM Extra 43'),(145,1,NULL,'20:00:00','Cena','Pollo',335.1010,11.1800,133.3280,426.5690,11.8430,46.5400,326.2740,2.5168,'Parmesan Tomato Chicken Dinner - NM Extra 44'),(146,1,NULL,'20:00:00','Cena','Pollo',217.1750,8.6734,74.4600,323.0170,9.5608,24.7979,73.4740,1.0251,'Dijon Chicken Celery Bowl - NM Extra 45'),(147,1,NULL,'20:00:00','Cena','Yogur',206.4000,3.7200,87.4500,88.3000,8.1750,34.8250,129.8500,1.9450,'Yogurt Chicken Romaine Dinner - NM Extra 46'),(148,1,NULL,'20:00:00','Cena','Pollo',291.6745,12.2694,100.7400,69.8970,10.4650,33.1315,44.0680,1.9573,'Herbed Chicken Pepper Plate - NM Extra 47'),(149,1,NULL,'20:00:00','Cena','Pollo',324.0120,12.2915,113.8800,85.5660,15.2490,37.9925,44.2130,1.8018,'Chicken Tomato Shallot Skillet - NM Extra 48'),(150,1,NULL,'20:00:00','Cena','Fruta',172.7625,3.0387,71.3575,53.9325,13.4640,23.6130,47.3450,1.6150,'Chicken Lettuce Apple Dinner - NM Extra 49'),(151,1,NULL,'20:00:00','Cena','Pollo',266.3000,7.8350,97.1500,370.7000,14.9250,33.6750,251.0000,1.7900,'Chicken Carrot Parmesan Bowl - NM Extra 50'),(152,1,NULL,'20:00:00','Cena','Pollo',228.0680,4.9887,104.9375,237.9925,10.4259,35.6765,68.0915,2.3805,'Bistro Chicken Tomato Salad - NM Extra 51'),(153,1,NULL,'20:00:00','Cena','Pollo',321.7240,13.6760,113.8800,76.8820,11.1540,36.7640,20.3710,1.3286,'Lemon Pepper Chicken Bowl - NM Extra 52'),(154,1,NULL,'20:00:00','Cena','Pollo',187.1700,5.2700,77.5625,213.7325,8.4405,25.7618,68.3145,1.7459,'Light Balsamic Chicken Greens - NM Extra 53'),(155,1,NULL,'20:00:00','Cena','Pollo',216.0900,4.1960,91.2500,501.4000,13.1320,30.6660,59.6200,1.1650,'Warm Dijon Carrot Chicken - NM Extra 54'),(156,1,NULL,'20:00:00','Cena','Pollo',244.2600,6.1180,109.0200,373.7500,7.6590,39.3990,216.2000,1.1730,'Chicken Tomato Cheese Dinner - NM Extra 55'),(157,1,NULL,'20:00:00','Cena','Fruta',301.0150,4.9140,121.5500,199.1600,19.4610,43.7905,130.3250,1.0660,'Chicken Apple Celery Plate - NM Extra 56'),(158,1,NULL,'20:00:00','Cena','Ensalada',236.8780,7.7350,91.0265,292.2895,9.0270,31.6752,221.0595,1.7714,'Parmesan Pepper Romaine Dinner - NM Extra 57'),(159,1,NULL,'20:00:00','Cena','Pollo',247.6200,11.5640,87.6000,62.9000,6.8500,28.8100,43.8800,1.6180,'Tomato Herb Chicken Supper - NM Extra 58'),(160,1,NULL,'20:00:00','Cena','Frutos secos',281.3705,11.5460,100.7400,74.3820,9.4127,36.0525,97.4855,2.6174,'Chicken Almond Romaine Dinner - NM Extra 59'),(161,1,NULL,'20:00:00','Cena','Pollo',366.3790,13.9620,118.6250,163.7220,19.6560,38.9740,58.0710,1.5951,'Carrot Pepper Chicken Dinner - NM Extra 60'),(162,1,NULL,'11:00:00','Colacion','Yogur',70.7625,0.5398,4.0375,29.7925,8.4703,8.8230,100.3850,0.3698,'Strawberry Yogurt Mini Cup - NM Extra 61'),(163,1,NULL,'11:00:00','Colacion','Yogur',98.7000,0.6000,4.5000,33.2000,14.8400,9.8300,103.8000,0.3300,'Blueberry Yogurt Mini Cup - NM Extra 62'),(164,1,NULL,'11:00:00','Colacion','Fruta',88.5170,0.3432,0.0000,1.7600,23.6610,0.5390,20.9220,0.2563,'Apple Cinnamon Snack - NM Extra 63'),(165,1,NULL,'11:00:00','Colacion','Frutos secos',116.0160,7.5060,0.0000,1.1640,11.0220,3.7380,55.0560,0.9408,'Almond Berry Snack - NM Extra 64'),(166,1,NULL,'11:00:00','Colacion','Frutos secos',96.3900,4.4285,0.0000,2.3375,14.0165,2.3927,9.0100,0.2507,'Peanut Butter Apple Bites - NM Extra 65'),(167,1,NULL,'11:00:00','Colacion','Yogur',105.2000,0.6100,5.0000,36.4500,16.0500,10.3150,111.3500,0.1600,'Yogurt Cranberry Cup - NM Extra 66'),(168,1,NULL,'11:00:00','Colacion','Frutos secos',107.6900,8.2170,0.0000,0.9900,9.1630,1.7050,23.5400,0.6710,'Pecan Strawberry Snack - NM Extra 67'),(169,1,NULL,'11:00:00','Colacion','Yogur',149.7360,4.1820,5.7000,44.2200,16.1100,13.7160,150.7800,0.7908,'Blueberry Flax Yogurt - NM Extra 68'),(170,1,NULL,'11:00:00','Colacion','Yogur',76.2450,0.6120,2.9750,134.1725,11.3645,7.1570,98.7700,0.4038,'Carrot Yogurt Dip - NM Extra 69'),(171,1,NULL,'11:00:00','Colacion','Frutos secos',115.6900,5.7000,0.0000,1.1100,16.2200,2.6100,35.5900,0.5070,'Apple Almond Mini Bowl - NM Extra 70'),(172,1,NULL,'11:00:00','Colacion','Huevo',79.8600,0.4510,0.0000,197.2300,5.0985,13.5795,19.0850,0.4455,'Egg White Tomato Snack - NM Extra 71'),(173,1,NULL,'11:00:00','Colacion','Yogur',158.3400,3.9480,5.1000,45.7200,19.0440,12.6060,130.2000,0.6780,'Berry Granola Mini Parfait - NM Extra 72'),(174,1,NULL,'11:00:00','Colacion','Frutos secos',92.8200,4.4795,0.0000,0.8500,12.9625,2.3205,27.4550,0.5440,'Blueberry Almond Bites - NM Extra 73'),(175,1,NULL,'11:00:00','Colacion','Frutos secos',96.4400,6.7500,0.0000,102.2100,6.3500,4.1250,55.5900,0.4970,'Celery Peanut Butter Sticks - NM Extra 74'),(176,1,NULL,'11:00:00','Colacion','Yogur',106.3150,0.6490,5.2250,39.3140,14.8500,11.3025,132.4290,0.4202,'Yogurt Maple Berry Cup - NM Extra 75'),(177,1,NULL,'11:00:00','Colacion','Fruta',123.0240,4.3080,0.0000,4.2600,21.8280,2.1420,32.7600,0.6852,'Apple Flax Mini Snack - NM Extra 76'),(178,1,NULL,'11:00:00','Colacion','Frutos secos',85.5015,5.8140,0.0000,1.0200,8.9250,1.4178,21.6750,0.5993,'Strawberry Pecan Cup - NM Extra 77'),(179,1,NULL,'11:00:00','Colacion','Avena',146.4500,2.9500,4.2500,37.3000,20.8900,10.4450,103.7000,0.5350,'Blueberry Granola Cup - NM Extra 78'),(180,1,NULL,'11:00:00','Colacion','Huevo',93.6100,0.5390,0.0000,204.8200,6.7870,14.1790,15.4000,0.5170,'Egg White Pepper Snack - NM Extra 79'),(181,1,NULL,'11:00:00','Colacion','Frutos secos',127.8960,5.6544,0.0000,1.4280,19.9080,2.6760,48.2760,0.6192,'Almond Apple Cinnamon Snack - NM Extra 80');
/*!40000 ALTER TABLE `comida` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dieta`
--

DROP TABLE IF EXISTS `dieta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dieta` (
  `ID_DIETA` int(11) NOT NULL,
  `ID_USUARIO` int(11) DEFAULT NULL,
  `ID_PACIENTE` int(11) DEFAULT NULL,
  `CALORIAS` decimal(9,4) DEFAULT NULL,
  `GRASAS` decimal(9,4) DEFAULT NULL,
  `COLESTEROL` decimal(9,4) DEFAULT NULL,
  `SODIO` decimal(9,4) DEFAULT NULL,
  `CARBOHIDRATOS` decimal(9,4) DEFAULT NULL,
  `PROTEINA` decimal(9,4) DEFAULT NULL,
  `CALCIO` decimal(9,4) DEFAULT NULL,
  `HIERRO` decimal(9,4) DEFAULT NULL,
  `COMENTARIO` varchar(300) DEFAULT NULL,
  `FECHA_CREACION` datetime DEFAULT NULL,
  `DIA_DESCANSO` varchar(10) DEFAULT NULL,
  `GENERO_DESTINATARIO` varchar(8) DEFAULT NULL,
  `CANTIDAD_COMIDAS` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`ID_DIETA`),
  KEY `FK_DIETA_USUARIO` (`ID_USUARIO`),
  KEY `FK_DIETA_PACIENTE` (`ID_PACIENTE`),
  CONSTRAINT `FK_DIETA_PACIENTE` FOREIGN KEY (`ID_PACIENTE`) REFERENCES `paciente` (`ID_PACIENTE`),
  CONSTRAINT `FK_DIETA_USUARIO` FOREIGN KEY (`ID_USUARIO`) REFERENCES `usuario` (`ID_USUARIO`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dieta`
--

LOCK TABLES `dieta` WRITE;
/*!40000 ALTER TABLE `dieta` DISABLE KEYS */;
INSERT INTO `dieta` VALUES (1,NULL,NULL,1200.0000,43.8134,109.5335,737.5254,141.1765,75.4564,396.7546,7.3022,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1200.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',2),(2,NULL,NULL,1400.0000,71.6016,205.3836,619.9192,90.4441,109.2867,NULL,NULL,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1400.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',2),(3,NULL,NULL,1600.0000,62.7027,659.4595,1848.6486,151.3514,114.5946,560.0000,8.6486,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1600.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',2),(4,NULL,NULL,1800.0000,97.0787,380.2247,1520.8989,101.1236,145.6180,357.9775,6.0674,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1800.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',3),(5,NULL,NULL,2000.0000,82.4972,323.2999,1212.9320,160.5351,167.2241,NULL,NULL,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 2000.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',3),(6,NULL,NULL,2200.0000,118.1976,684.4021,1860.6586,160.1386,141.0745,812.1317,11.4385,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 2200.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',3),(7,NULL,NULL,1500.0000,87.8726,226.6187,679.8561,90.9558,101.7472,464.0288,7.7081,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1500.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',3),(8,NULL,NULL,1300.0000,47.4020,173.0173,918.4139,129.1705,105.4695,NULL,NULL,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1300.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',4),(9,NULL,NULL,1700.0000,90.0976,531.1018,1493.7239,110.2510,120.9205,526.3598,8.2985,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1700.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',4),(10,NULL,NULL,1900.0000,74.7033,510.2374,1706.8991,184.6439,143.7685,NULL,NULL,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1900.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',4),(11,NULL,NULL,2100.0000,98.1756,603.4208,1872.5200,149.6579,168.8141,NULL,NULL,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 2100.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',5),(12,NULL,NULL,2300.0000,115.4699,606.8885,1740.1051,173.2049,159.7782,751.8973,12.0841,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 2300.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',5),(13,NULL,NULL,1400.0000,64.2715,268.2635,1050.6986,95.9415,119.2282,NULL,NULL,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1400.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',5),(14,NULL,NULL,2500.0000,109.3035,584.6467,2011.9471,221.1490,183.0198,NULL,NULL,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 2500.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',6),(15,NULL,NULL,1800.0000,72.6796,429.4704,1445.0970,166.1248,138.7520,NULL,NULL,'Datos de prueba basados en recetas de Eat This Much. Objetivo: 1800.0000 kcal. Cantidades escaladas proporcionalmente.','2026-10-09 19:44:21','No','Todos',6);
/*!40000 ALTER TABLE `dieta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ingrediente`
--

DROP TABLE IF EXISTS `ingrediente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ingrediente` (
  `ID_INGREDIENTE` int(11) NOT NULL,
  `NOMBRE` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`ID_INGREDIENTE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ingrediente`
--

LOCK TABLES `ingrediente` WRITE;
/*!40000 ALTER TABLE `ingrediente` DISABLE KEYS */;
INSERT INTO `ingrediente` VALUES (1,'Almonds'),(2,'American cheese, low fat'),(3,'Apple'),(4,'Balsamic vinaigrette, low fat'),(5,'Bay leaf'),(6,'Black pepper'),(7,'Blueberries'),(8,'Carrots'),(9,'Celery'),(10,'Chicken breast, raw skinless'),(11,'Chicken stock'),(12,'Cinnamon'),(13,'Dijon mustard'),(14,'Dried cranberries, sweetened'),(15,'Egg'),(16,'Egg white'),(17,'Flaxseed'),(18,'Granola with almonds'),(19,'Greek yogurt, plain nonfat'),(20,'Lemon juice'),(21,'Maple syrup'),(22,'Mayonnaise, regular salted'),(23,'Olive oil'),(24,'Parmesan cheese'),(25,'Peanut butter, unsalted'),(26,'Pecans'),(27,'Red bell pepper'),(28,'Rolled oats'),(29,'Romaine lettuce'),(30,'Shallots'),(31,'Strawberries'),(32,'Tarragon, dried'),(33,'Tomatoes'),(34,'Water');
/*!40000 ALTER TABLE `ingrediente` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ingrediente_comida`
--

DROP TABLE IF EXISTS `ingrediente_comida`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ingrediente_comida` (
  `ID_INGREDIENTE` int(11) NOT NULL,
  `ID_COMIDA` int(11) NOT NULL,
  `CANTIDAD` decimal(6,2) DEFAULT NULL,
  PRIMARY KEY (`ID_INGREDIENTE`,`ID_COMIDA`),
  KEY `FK_INGREDIENTE_COMIDA_COM` (`ID_COMIDA`),
  CONSTRAINT `FK_INGREDIENTE_COMIDA_COM` FOREIGN KEY (`ID_COMIDA`) REFERENCES `comida` (`ID_COMIDA`),
  CONSTRAINT `FK_INGREDIENTE_COMIDA_ING` FOREIGN KEY (`ID_INGREDIENTE`) REFERENCES `ingrediente` (`ID_INGREDIENTE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ingrediente_comida`
--

LOCK TABLES `ingrediente_comida` WRITE;
/*!40000 ALTER TABLE `ingrediente_comida` DISABLE KEYS */;
INSERT INTO `ingrediente_comida` VALUES (1,4,25.44),(1,8,27.30),(1,14,25.74),(1,17,20.81),(1,26,16.00),(1,33,16.16),(1,38,18.13),(1,43,12.57),(1,49,17.16),(1,103,18.00),(1,113,18.20),(1,119,20.00),(1,127,15.00),(1,133,24.70),(1,140,20.70),(1,160,14.95),(1,165,14.40),(1,171,11.00),(1,174,8.50),(1,181,10.80),(2,5,45.41),(2,13,40.03),(2,23,24.90),(2,27,29.60),(2,31,25.14),(2,36,28.20),(2,46,26.69),(2,52,19.82),(2,116,27.60),(2,120,34.50),(2,156,27.60),(3,5,240.00),(3,13,211.61),(3,23,131.59),(3,27,156.45),(3,31,132.90),(3,36,149.04),(3,46,141.08),(3,52,104.77),(3,104,126.50),(3,110,93.50),(3,115,120.00),(3,119,70.00),(3,121,130.00),(3,127,85.00),(3,138,76.50),(3,150,63.75),(3,157,97.50),(3,164,165.00),(3,166,89.25),(3,171,100.00),(3,177,138.00),(3,181,120.00),(4,9,30.34),(4,22,17.78),(4,29,21.14),(4,35,17.96),(4,45,13.97),(4,51,19.06),(4,57,14.16),(4,126,27.20),(4,138,25.50),(4,144,28.75),(4,154,20.40),(5,6,0.07),(5,12,0.07),(5,24,0.04),(5,34,0.04),(5,40,0.04),(5,44,0.03),(5,55,0.03),(6,4,0.24),(6,8,0.25),(6,14,0.24),(6,17,0.19),(6,26,0.15),(6,33,0.15),(6,38,0.17),(6,43,0.12),(6,49,0.16),(6,143,1.00),(7,1,377.28),(7,3,139.43),(7,7,313.48),(7,10,164.99),(7,19,87.69),(7,20,183.68),(7,28,104.30),(7,32,88.60),(7,41,68.93),(7,42,144.38),(7,47,94.05),(7,48,197.00),(7,54,69.85),(7,102,55.25),(7,107,65.00),(7,111,90.00),(7,113,65.00),(7,118,76.50),(7,163,80.00),(7,169,66.00),(7,174,76.50),(7,179,70.00),(8,2,148.48),(8,11,136.01),(8,15,116.29),(8,18,94.04),(8,21,72.29),(8,25,72.32),(8,30,85.98),(8,39,81.90),(8,50,77.53),(8,56,57.58),(8,123,100.00),(8,126,68.00),(8,130,68.00),(8,134,93.50),(8,139,100.00),(8,143,130.00),(8,146,59.50),(8,151,120.00),(8,155,125.00),(8,161,123.50),(8,170,93.50),(9,4,18.84),(9,8,20.22),(9,14,19.06),(9,17,15.42),(9,26,11.85),(9,33,11.97),(9,38,13.43),(9,43,9.31),(9,49,12.71),(9,129,123.50),(9,135,80.00),(9,140,126.50),(9,146,102.00),(9,154,55.25),(9,157,130.00),(9,175,125.00),(10,2,138.74),(10,4,222.34),(10,6,245.04),(10,8,238.65),(10,9,238.65),(10,11,127.09),(10,12,252.69),(10,14,224.96),(10,15,108.67),(10,17,181.91),(10,18,87.87),(10,21,67.55),(10,22,139.84),(10,24,134.36),(10,25,67.57),(10,26,139.89),(10,29,166.32),(10,30,80.34),(10,33,141.28),(10,34,135.69),(10,35,141.28),(10,38,158.44),(10,39,76.53),(10,40,152.17),(10,43,109.91),(10,44,105.57),(10,45,109.91),(10,49,149.97),(10,50,72.45),(10,51,149.97),(10,55,106.97),(10,56,53.80),(10,57,111.38),(10,122,106.25),(10,123,130.00),(10,124,149.50),(10,125,175.50),(10,126,114.75),(10,127,125.00),(10,128,166.75),(10,129,182.00),(10,130,114.75),(10,131,140.00),(10,132,166.75),(10,133,169.00),(10,134,119.00),(10,135,145.00),(10,136,161.00),(10,137,182.00),(10,138,119.00),(10,139,145.00),(10,140,161.00),(10,141,188.50),(10,142,93.50),(10,143,115.00),(10,144,132.25),(10,145,156.00),(10,146,102.00),(10,147,115.00),(10,148,138.00),(10,149,156.00),(10,150,97.75),(10,151,115.00),(10,152,143.75),(10,153,156.00),(10,154,106.25),(10,155,125.00),(10,156,138.00),(10,157,162.50),(10,158,106.25),(10,159,120.00),(10,160,138.00),(10,161,162.50),(11,6,172.97),(11,12,178.37),(11,24,94.84),(11,34,95.78),(11,40,107.41),(11,44,74.52),(11,55,75.51),(11,143,90.00),(11,155,80.00),(12,5,4.32),(12,13,3.81),(12,23,2.37),(12,27,2.82),(12,31,2.39),(12,36,2.69),(12,46,2.54),(12,52,1.89),(12,102,1.70),(12,108,1.15),(12,110,1.70),(12,115,1.00),(12,119,2.00),(12,164,1.10),(12,181,1.20),(13,6,5.41),(13,12,5.57),(13,24,2.96),(13,34,2.99),(13,40,3.36),(13,44,2.33),(13,55,2.36),(13,124,18.40),(13,128,17.25),(13,135,17.00),(13,146,13.60),(13,152,13.80),(13,155,14.00),(13,170,4.25),(14,6,10.81),(14,12,11.15),(14,24,5.93),(14,34,5.99),(14,40,6.71),(14,44,4.66),(14,55,4.72),(14,114,18.70),(14,130,18.70),(14,167,15.00),(15,5,121.08),(15,13,106.76),(15,23,66.39),(15,27,78.93),(15,31,67.05),(15,36,75.19),(15,46,71.17),(15,52,52.86),(15,106,85.00),(15,116,115.00),(16,1,194.73),(16,5,142.70),(16,7,161.80),(16,13,125.82),(16,20,94.80),(16,23,78.24),(16,27,93.03),(16,31,79.02),(16,36,88.62),(16,42,74.52),(16,46,83.88),(16,48,101.68),(16,52,62.30),(16,105,221.00),(16,112,207.00),(16,120,178.25),(16,172,115.50),(16,180,121.00),(17,2,17.04),(17,11,15.61),(17,15,13.34),(17,18,10.79),(17,21,8.30),(17,25,8.30),(17,30,9.87),(17,39,9.40),(17,50,8.90),(17,56,6.61),(17,109,18.20),(17,115,13.00),(17,169,8.40),(17,177,9.60),(18,3,54.64),(18,10,64.66),(18,19,34.37),(18,28,40.88),(18,32,34.72),(18,41,27.01),(18,47,36.86),(18,54,27.37),(18,107,35.00),(18,110,21.25),(18,118,25.50),(18,173,16.80),(18,179,12.00),(19,3,427.73),(19,10,506.13),(19,16,131.04),(19,19,269.01),(19,28,319.96),(19,32,271.78),(19,37,114.13),(19,41,211.44),(19,47,288.51),(19,53,80.23),(19,54,214.26),(19,102,93.50),(19,103,170.00),(19,107,160.00),(19,109,97.50),(19,110,136.00),(19,111,175.00),(19,114,136.00),(19,117,78.00),(19,118,119.00),(19,121,221.00),(19,129,91.00),(19,137,97.50),(19,147,70.00),(19,157,58.50),(19,162,80.75),(19,163,90.00),(19,167,100.00),(19,169,114.00),(19,170,59.50),(19,173,102.00),(19,176,104.50),(19,179,85.00),(20,4,7.07),(20,6,5.41),(20,8,7.58),(20,12,5.57),(20,14,7.15),(20,17,5.78),(20,24,2.96),(20,26,4.45),(20,33,4.49),(20,34,2.99),(20,38,5.04),(20,40,3.36),(20,43,3.49),(20,44,2.33),(20,49,4.77),(20,55,2.36),(20,122,17.00),(20,127,15.00),(20,134,18.70),(20,141,20.80),(20,142,17.00),(20,147,15.00),(20,150,12.75),(20,153,26.00),(20,160,17.25),(21,5,43.24),(21,13,38.13),(21,23,23.71),(21,27,28.19),(21,31,23.95),(21,36,26.85),(21,46,25.42),(21,52,18.88),(21,108,11.50),(21,117,15.60),(21,176,7.70),(22,4,51.82),(22,6,19.82),(22,8,55.62),(22,12,20.44),(22,14,52.43),(22,17,42.39),(22,24,10.87),(22,26,32.60),(22,33,32.92),(22,34,10.98),(22,38,36.92),(22,40,12.31),(22,43,25.62),(22,44,8.54),(22,49,34.95),(22,55,8.65),(23,2,21.91),(23,5,15.14),(23,9,18.20),(23,11,20.07),(23,13,13.34),(23,15,17.16),(23,18,13.87),(23,21,10.67),(23,22,10.67),(23,23,8.30),(23,25,10.67),(23,27,9.87),(23,29,12.69),(23,30,12.69),(23,31,8.38),(23,35,10.78),(23,36,9.40),(23,39,12.08),(23,45,8.38),(23,46,8.90),(23,50,11.44),(23,51,11.44),(23,52,6.61),(23,56,8.50),(23,57,8.50),(23,105,6.50),(23,106,4.25),(23,112,4.60),(23,122,7.65),(23,123,8.00),(23,124,11.50),(23,125,9.10),(23,128,8.05),(23,130,5.95),(23,131,10.00),(23,134,6.80),(23,135,9.00),(23,139,8.00),(23,141,7.80),(23,142,4.25),(23,143,5.00),(23,146,5.10),(23,148,8.05),(23,149,7.80),(23,153,9.10),(23,159,8.00),(23,161,9.10),(24,2,12.17),(24,9,10.11),(24,11,11.15),(24,15,9.53),(24,18,7.71),(24,21,5.93),(24,22,5.93),(24,25,5.93),(24,29,7.05),(24,30,7.05),(24,35,5.99),(24,39,6.71),(24,45,4.66),(24,50,6.35),(24,51,6.35),(24,56,4.72),(24,57,4.72),(24,112,18.40),(24,125,28.60),(24,132,21.85),(24,136,20.70),(24,141,28.60),(24,145,22.10),(24,151,15.00),(24,158,15.30),(25,16,24.67),(25,37,21.48),(25,53,15.10),(25,104,16.10),(25,111,18.00),(25,121,20.80),(25,166,8.50),(25,175,13.00),(26,6,9.73),(26,12,10.03),(26,24,5.33),(26,34,5.39),(26,40,6.04),(26,44,4.19),(26,55,4.25),(26,108,17.25),(26,114,11.90),(26,168,11.00),(26,178,7.65),(27,2,180.12),(27,11,164.99),(27,15,141.07),(27,18,114.08),(27,21,87.69),(27,25,87.73),(27,30,104.30),(27,39,99.36),(27,50,94.05),(27,56,69.85),(27,105,104.00),(27,116,103.50),(27,123,110.00),(27,128,138.00),(27,132,126.50),(27,136,149.50),(27,144,143.75),(27,148,155.25),(27,153,156.00),(27,158,93.50),(27,161,130.00),(27,180,99.00),(28,1,63.29),(28,5,86.49),(28,7,52.58),(28,13,76.26),(28,16,61.66),(28,20,30.81),(28,23,47.42),(28,27,56.38),(28,31,47.89),(28,36,53.71),(28,37,53.71),(28,42,24.22),(28,46,50.84),(28,48,33.05),(28,52,37.76),(28,53,37.76),(28,102,38.25),(28,104,51.75),(28,108,49.45),(28,109,54.60),(28,113,55.90),(28,115,40.00),(28,117,55.90),(28,119,44.00),(29,2,114.40),(29,9,190.11),(29,11,104.79),(29,15,89.60),(29,18,72.46),(29,21,55.70),(29,22,111.39),(29,25,55.72),(29,29,132.49),(29,30,66.25),(29,35,112.54),(29,39,63.11),(29,45,87.56),(29,50,59.74),(29,51,119.47),(29,56,44.36),(29,57,88.73),(29,106,42.50),(29,122,93.50),(29,124,161.00),(29,126,119.00),(29,127,120.00),(29,128,103.50),(29,129,130.00),(29,130,93.50),(29,132,138.00),(29,133,169.00),(29,134,97.75),(29,137,156.00),(29,138,102.00),(29,140,103.50),(29,141,169.00),(29,142,85.00),(29,144,103.50),(29,145,117.00),(29,147,140.00),(29,150,114.75),(29,151,85.00),(29,152,115.00),(29,154,119.00),(29,158,85.00),(29,160,149.50),(30,6,3.60),(30,12,3.72),(30,24,1.98),(30,34,2.00),(30,40,2.24),(30,44,1.55),(30,55,1.57),(30,131,40.00),(30,149,45.50),(31,3,143.20),(31,10,169.45),(31,19,90.06),(31,28,107.12),(31,32,90.99),(31,41,70.79),(31,47,96.59),(31,54,71.74),(31,103,100.00),(31,109,156.00),(31,113,84.50),(31,117,162.50),(31,162,72.25),(31,165,102.00),(31,168,99.00),(31,173,60.00),(31,176,77.00),(31,178,102.00),(32,6,0.72),(32,12,0.74),(32,24,0.39),(32,34,0.40),(32,40,0.45),(32,44,0.31),(32,55,0.31),(32,137,2.60),(32,148,2.30),(32,159,2.00),(33,2,299.39),(33,11,274.25),(33,15,234.49),(33,18,189.62),(33,21,145.76),(33,25,145.82),(33,30,173.37),(33,39,165.15),(33,50,156.33),(33,56,116.10),(33,105,91.00),(33,106,110.50),(33,112,115.00),(33,120,138.00),(33,122,76.50),(33,124,92.00),(33,125,156.00),(33,131,140.00),(33,133,117.00),(33,135,120.00),(33,136,115.00),(33,139,140.00),(33,142,102.00),(33,145,182.00),(33,149,195.00),(33,152,149.50),(33,156,161.00),(33,159,150.00),(33,172,110.00),(34,1,433.27),(34,7,360.00),(34,16,365.36),(34,20,210.94),(34,37,318.21),(34,42,165.80),(34,48,226.23),(34,53,223.70);
/*!40000 ALTER TABLE `ingrediente_comida` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `paciente`
--

DROP TABLE IF EXISTS `paciente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `paciente` (
  `ID_PACIENTE` int(11) NOT NULL,
  `NOMBRE` varchar(50) DEFAULT NULL,
  `ALTURA` decimal(5,2) DEFAULT NULL,
  `PESO` decimal(5,2) DEFAULT NULL,
  `EDAD` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID_PACIENTE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `paciente`
--

LOCK TABLES `paciente` WRITE;
/*!40000 ALTER TABLE `paciente` DISABLE KEYS */;
INSERT INTO `paciente` VALUES (1,'Carlos Trejo',178.00,89.00,19),(2,'Carlos Trejo',178.00,89.00,19),(3,'Luis Arjona',1.78,89.00,19),(4,'Eduardo Barragan',168.00,87.00,27);
/*!40000 ALTER TABLE `paciente` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `ID_USUARIO` int(11) NOT NULL,
  `PASSWORD` varchar(100) DEFAULT NULL,
  `CORREO` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`ID_USUARIO`),
  UNIQUE KEY `UNIQ_CORREO` (`CORREO`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES (1,'$2a$10$m9knbqZRnceAovzzCFTOTuOrFjxk2I9UaagyDsxQi1zepNzn8H4SC','probandoregistro@cactus.moy');
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'nutrimaker'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-23 14:11:09
