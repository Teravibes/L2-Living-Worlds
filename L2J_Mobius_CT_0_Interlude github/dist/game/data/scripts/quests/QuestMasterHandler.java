/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package quests;

import java.util.logging.Level;
import java.util.logging.Logger;

import quests.Q00001_LettersOfLove.Q00001_LettersOfLove;
import quests.Q00002_WhatWomenWant.Q00002_WhatWomenWant;
import quests.Q00003_WillTheSealBeBroken.Q00003_WillTheSealBeBroken;
import quests.Q00004_LongLiveThePaagrioLord.Q00004_LongLiveThePaagrioLord;
import quests.Q00005_MinersFavor.Q00005_MinersFavor;
import quests.Q00006_StepIntoTheFuture.Q00006_StepIntoTheFuture;
import quests.Q00007_ATripBegins.Q00007_ATripBegins;
import quests.Q00008_AnAdventureBegins.Q00008_AnAdventureBegins;
import quests.Q00009_IntoTheCityOfHumans.Q00009_IntoTheCityOfHumans;
import quests.Q00010_IntoTheWorld.Q00010_IntoTheWorld;
import quests.Q00011_SecretMeetingWithKetraOrcs.Q00011_SecretMeetingWithKetraOrcs;
import quests.Q00012_SecretMeetingWithVarkaSilenos.Q00012_SecretMeetingWithVarkaSilenos;
import quests.Q00013_ParcelDelivery.Q00013_ParcelDelivery;
import quests.Q00014_WhereaboutsOfTheArchaeologist.Q00014_WhereaboutsOfTheArchaeologist;
import quests.Q00015_SweetWhispers.Q00015_SweetWhispers;
import quests.Q00016_TheComingDarkness.Q00016_TheComingDarkness;
import quests.Q00017_LightAndDarkness.Q00017_LightAndDarkness;
import quests.Q00018_MeetingWithTheGoldenRam.Q00018_MeetingWithTheGoldenRam;
import quests.Q00019_GoToThePastureland.Q00019_GoToThePastureland;
import quests.Q00020_BringUpWithLove.Q00020_BringUpWithLove;
import quests.Q00021_HiddenTruth.Q00021_HiddenTruth;
import quests.Q00022_TragedyInVonHellmannForest.Q00022_TragedyInVonHellmannForest;
import quests.Q00023_LidiasHeart.Q00023_LidiasHeart;
import quests.Q00024_InhabitantsOfTheForestOfTheDead.Q00024_InhabitantsOfTheForestOfTheDead;
import quests.Q00025_HidingBehindTheTruth.Q00025_HidingBehindTheTruth;
import quests.Q00027_ChestCaughtWithABaitOfWind.Q00027_ChestCaughtWithABaitOfWind;
import quests.Q00028_ChestCaughtWithABaitOfIcyAir.Q00028_ChestCaughtWithABaitOfIcyAir;
import quests.Q00029_ChestCaughtWithABaitOfEarth.Q00029_ChestCaughtWithABaitOfEarth;
import quests.Q00030_ChestCaughtWithABaitOfFire.Q00030_ChestCaughtWithABaitOfFire;
import quests.Q00031_SecretBuriedInTheSwamp.Q00031_SecretBuriedInTheSwamp;
import quests.Q00032_AnObviousLie.Q00032_AnObviousLie;
import quests.Q00033_MakeAPairOfDressShoes.Q00033_MakeAPairOfDressShoes;
import quests.Q00034_InSearchOfCloth.Q00034_InSearchOfCloth;
import quests.Q00035_FindGlitteringJewelry.Q00035_FindGlitteringJewelry;
import quests.Q00036_MakeASewingKit.Q00036_MakeASewingKit;
import quests.Q00037_MakeFormalWear.Q00037_MakeFormalWear;
import quests.Q00038_DragonFangs.Q00038_DragonFangs;
import quests.Q00039_RedEyedInvaders.Q00039_RedEyedInvaders;
import quests.Q00042_HelpTheUncle.Q00042_HelpTheUncle;
import quests.Q00043_HelpTheSister.Q00043_HelpTheSister;
import quests.Q00044_HelpTheSon.Q00044_HelpTheSon;
import quests.Q00045_ToTalkingIsland.Q00045_ToTalkingIsland;
import quests.Q00046_OnceMoreInTheArmsOfTheMotherTree.Q00046_OnceMoreInTheArmsOfTheMotherTree;
import quests.Q00047_IntoTheDarkElvenForest.Q00047_IntoTheDarkElvenForest;
import quests.Q00048_ToTheImmortalPlateau.Q00048_ToTheImmortalPlateau;
import quests.Q00049_TheRoadHome.Q00049_TheRoadHome;
import quests.Q00050_LanoscosSpecialBait.Q00050_LanoscosSpecialBait;
import quests.Q00051_OFullesSpecialBait.Q00051_OFullesSpecialBait;
import quests.Q00052_WilliesSpecialBait.Q00052_WilliesSpecialBait;
import quests.Q00053_LinnaeusSpecialBait.Q00053_LinnaeusSpecialBait;
import quests.Q00070_SagaOfThePhoenixKnight.Q00070_SagaOfThePhoenixKnight;
import quests.Q00071_SagaOfEvasTemplar.Q00071_SagaOfEvasTemplar;
import quests.Q00072_SagaOfTheSwordMuse.Q00072_SagaOfTheSwordMuse;
import quests.Q00073_SagaOfTheDuelist.Q00073_SagaOfTheDuelist;
import quests.Q00074_SagaOfTheDreadnought.Q00074_SagaOfTheDreadnought;
import quests.Q00075_SagaOfTheTitan.Q00075_SagaOfTheTitan;
import quests.Q00076_SagaOfTheGrandKhavatari.Q00076_SagaOfTheGrandKhavatari;
import quests.Q00077_SagaOfTheDominator.Q00077_SagaOfTheDominator;
import quests.Q00078_SagaOfTheDoomcryer.Q00078_SagaOfTheDoomcryer;
import quests.Q00079_SagaOfTheAdventurer.Q00079_SagaOfTheAdventurer;
import quests.Q00080_SagaOfTheWindRider.Q00080_SagaOfTheWindRider;
import quests.Q00081_SagaOfTheGhostHunter.Q00081_SagaOfTheGhostHunter;
import quests.Q00082_SagaOfTheSagittarius.Q00082_SagaOfTheSagittarius;
import quests.Q00083_SagaOfTheMoonlightSentinel.Q00083_SagaOfTheMoonlightSentinel;
import quests.Q00084_SagaOfTheGhostSentinel.Q00084_SagaOfTheGhostSentinel;
import quests.Q00085_SagaOfTheCardinal.Q00085_SagaOfTheCardinal;
import quests.Q00086_SagaOfTheHierophant.Q00086_SagaOfTheHierophant;
import quests.Q00087_SagaOfEvasSaint.Q00087_SagaOfEvasSaint;
import quests.Q00088_SagaOfTheArchmage.Q00088_SagaOfTheArchmage;
import quests.Q00089_SagaOfTheMysticMuse.Q00089_SagaOfTheMysticMuse;
import quests.Q00090_SagaOfTheStormScreamer.Q00090_SagaOfTheStormScreamer;
import quests.Q00091_SagaOfTheArcanaLord.Q00091_SagaOfTheArcanaLord;
import quests.Q00092_SagaOfTheElementalMaster.Q00092_SagaOfTheElementalMaster;
import quests.Q00093_SagaOfTheSpectralMaster.Q00093_SagaOfTheSpectralMaster;
import quests.Q00094_SagaOfTheSoultaker.Q00094_SagaOfTheSoultaker;
import quests.Q00095_SagaOfTheHellKnight.Q00095_SagaOfTheHellKnight;
import quests.Q00096_SagaOfTheSpectralDancer.Q00096_SagaOfTheSpectralDancer;
import quests.Q00097_SagaOfTheShillienTemplar.Q00097_SagaOfTheShillienTemplar;
import quests.Q00098_SagaOfTheShillienSaint.Q00098_SagaOfTheShillienSaint;
import quests.Q00099_SagaOfTheFortuneSeeker.Q00099_SagaOfTheFortuneSeeker;
import quests.Q00100_SagaOfTheMaestro.Q00100_SagaOfTheMaestro;
import quests.Q00101_SwordOfSolidarity.Q00101_SwordOfSolidarity;
import quests.Q00102_SeaOfSporesFever.Q00102_SeaOfSporesFever;
import quests.Q00103_SpiritOfCraftsman.Q00103_SpiritOfCraftsman;
import quests.Q00104_SpiritOfMirrors.Q00104_SpiritOfMirrors;
import quests.Q00105_SkirmishWithOrcs.Q00105_SkirmishWithOrcs;
import quests.Q00106_ForgottenTruth.Q00106_ForgottenTruth;
import quests.Q00107_MercilessPunishment.Q00107_MercilessPunishment;
import quests.Q00108_JumbleTumbleDiamondFuss.Q00108_JumbleTumbleDiamondFuss;
import quests.Q00109_InSearchOfTheNest.Q00109_InSearchOfTheNest;
import quests.Q00110_ToThePrimevalIsle.Q00110_ToThePrimevalIsle;
import quests.Q00111_ElrokianHuntersProof.Q00111_ElrokianHuntersProof;
import quests.Q00112_WalkOfFate.Q00112_WalkOfFate;
import quests.Q00113_StatusOfTheBeaconTower.Q00113_StatusOfTheBeaconTower;
import quests.Q00114_ResurrectionOfAnOldManager.Q00114_ResurrectionOfAnOldManager;
import quests.Q00115_TheOtherSideOfTruth.Q00115_TheOtherSideOfTruth;
import quests.Q00116_BeyondTheHillsOfWinter.Q00116_BeyondTheHillsOfWinter;
import quests.Q00117_TheOceanOfDistantStars.Q00117_TheOceanOfDistantStars;
import quests.Q00118_ToLeadAndBeLed.Q00118_ToLeadAndBeLed;
import quests.Q00119_LastImperialPrince.Q00119_LastImperialPrince;
import quests.Q00120_PavelsLastResearch.Q00120_PavelsLastResearch;
import quests.Q00121_PavelTheGiant.Q00121_PavelTheGiant;
import quests.Q00122_OminousNews.Q00122_OminousNews;
import quests.Q00123_TheLeaderAndTheFollower.Q00123_TheLeaderAndTheFollower;
import quests.Q00124_MeetingTheElroki.Q00124_MeetingTheElroki;
import quests.Q00125_TheNameOfEvil1.Q00125_TheNameOfEvil1;
import quests.Q00126_TheNameOfEvil2.Q00126_TheNameOfEvil2;
import quests.Q00127_KamaelAWindowToTheFuture.Q00127_KamaelAWindowToTheFuture;
import quests.Q00151_CureForFever.Q00151_CureForFever;
import quests.Q00152_ShardsOfGolem.Q00152_ShardsOfGolem;
import quests.Q00153_DeliverGoods.Q00153_DeliverGoods;
import quests.Q00154_SacrificeToTheSea.Q00154_SacrificeToTheSea;
import quests.Q00155_FindSirWindawood.Q00155_FindSirWindawood;
import quests.Q00156_MillenniumLove.Q00156_MillenniumLove;
import quests.Q00157_RecoverSmuggledGoods.Q00157_RecoverSmuggledGoods;
import quests.Q00158_SeedOfEvil.Q00158_SeedOfEvil;
import quests.Q00159_ProtectTheWaterSource.Q00159_ProtectTheWaterSource;
import quests.Q00160_NerupasRequest.Q00160_NerupasRequest;
import quests.Q00161_FruitOfTheMotherTree.Q00161_FruitOfTheMotherTree;
import quests.Q00162_CurseOfTheUndergroundFortress.Q00162_CurseOfTheUndergroundFortress;
import quests.Q00163_LegacyOfThePoet.Q00163_LegacyOfThePoet;
import quests.Q00164_BloodFiend.Q00164_BloodFiend;
import quests.Q00165_ShilensHunt.Q00165_ShilensHunt;
import quests.Q00166_MassOfDarkness.Q00166_MassOfDarkness;
import quests.Q00167_DwarvenKinship.Q00167_DwarvenKinship;
import quests.Q00168_DeliverSupplies.Q00168_DeliverSupplies;
import quests.Q00169_OffspringOfNightmares.Q00169_OffspringOfNightmares;
import quests.Q00170_DangerousSeduction.Q00170_DangerousSeduction;
import quests.Q00171_ActsOfEvil.Q00171_ActsOfEvil;
import quests.Q00211_TrialOfTheChallenger.Q00211_TrialOfTheChallenger;
import quests.Q00212_TrialOfDuty.Q00212_TrialOfDuty;
import quests.Q00213_TrialOfTheSeeker.Q00213_TrialOfTheSeeker;
import quests.Q00214_TrialOfTheScholar.Q00214_TrialOfTheScholar;
import quests.Q00215_TrialOfThePilgrim.Q00215_TrialOfThePilgrim;
import quests.Q00216_TrialOfTheGuildsman.Q00216_TrialOfTheGuildsman;
import quests.Q00217_TestimonyOfTrust.Q00217_TestimonyOfTrust;
import quests.Q00218_TestimonyOfLife.Q00218_TestimonyOfLife;
import quests.Q00219_TestimonyOfFate.Q00219_TestimonyOfFate;
import quests.Q00220_TestimonyOfGlory.Q00220_TestimonyOfGlory;
import quests.Q00221_TestimonyOfProsperity.Q00221_TestimonyOfProsperity;
import quests.Q00222_TestOfTheDuelist.Q00222_TestOfTheDuelist;
import quests.Q00223_TestOfTheChampion.Q00223_TestOfTheChampion;
import quests.Q00224_TestOfSagittarius.Q00224_TestOfSagittarius;
import quests.Q00225_TestOfTheSearcher.Q00225_TestOfTheSearcher;
import quests.Q00226_TestOfTheHealer.Q00226_TestOfTheHealer;
import quests.Q00227_TestOfTheReformer.Q00227_TestOfTheReformer;
import quests.Q00228_TestOfMagus.Q00228_TestOfMagus;
import quests.Q00229_TestOfWitchcraft.Q00229_TestOfWitchcraft;
import quests.Q00230_TestOfTheSummoner.Q00230_TestOfTheSummoner;
import quests.Q00231_TestOfTheMaestro.Q00231_TestOfTheMaestro;
import quests.Q00232_TestOfTheLord.Q00232_TestOfTheLord;
import quests.Q00233_TestOfTheWarSpirit.Q00233_TestOfTheWarSpirit;
import quests.Q00234_FatesWhisper.Q00234_FatesWhisper;
import quests.Q00235_MimirsElixir.Q00235_MimirsElixir;
import quests.Q00241_PossessorOfAPreciousSoul1.Q00241_PossessorOfAPreciousSoul1;
import quests.Q00242_PossessorOfAPreciousSoul2.Q00242_PossessorOfAPreciousSoul2;
import quests.Q00246_PossessorOfAPreciousSoul3.Q00246_PossessorOfAPreciousSoul3;
import quests.Q00247_PossessorOfAPreciousSoul4.Q00247_PossessorOfAPreciousSoul4;
import quests.Q00255_Tutorial.Q00255_Tutorial;
import quests.Q00257_TheGuardIsBusy.Q00257_TheGuardIsBusy;
import quests.Q00258_BringWolfPelts.Q00258_BringWolfPelts;
import quests.Q00259_RequestFromTheFarmOwner.Q00259_RequestFromTheFarmOwner;
import quests.Q00260_OrcHunting.Q00260_OrcHunting;
import quests.Q00261_CollectorsDream.Q00261_CollectorsDream;
import quests.Q00262_TradeWithTheIvoryTower.Q00262_TradeWithTheIvoryTower;
import quests.Q00263_OrcSubjugation.Q00263_OrcSubjugation;
import quests.Q00264_KeenClaws.Q00264_KeenClaws;
import quests.Q00265_BondsOfSlavery.Q00265_BondsOfSlavery;
import quests.Q00266_PleasOfPixies.Q00266_PleasOfPixies;
import quests.Q00267_WrathOfVerdure.Q00267_WrathOfVerdure;
import quests.Q00271_ProofOfValor.Q00271_ProofOfValor;
import quests.Q00272_WrathOfAncestors.Q00272_WrathOfAncestors;
import quests.Q00273_InvadersOfTheHolyLand.Q00273_InvadersOfTheHolyLand;
import quests.Q00274_SkirmishWithTheWerewolves.Q00274_SkirmishWithTheWerewolves;
import quests.Q00275_DarkWingedSpies.Q00275_DarkWingedSpies;
import quests.Q00276_TotemOfTheHestui.Q00276_TotemOfTheHestui;
import quests.Q00277_GatekeepersOffering.Q00277_GatekeepersOffering;
import quests.Q00291_RevengeOfTheRedbonnet.Q00291_RevengeOfTheRedbonnet;
import quests.Q00292_BrigandsSweep.Q00292_BrigandsSweep;
import quests.Q00293_TheHiddenVeins.Q00293_TheHiddenVeins;
import quests.Q00294_CovertBusiness.Q00294_CovertBusiness;
import quests.Q00295_DreamingOfTheSkies.Q00295_DreamingOfTheSkies;
import quests.Q00296_TarantulasSpiderSilk.Q00296_TarantulasSpiderSilk;
import quests.Q00297_GatekeepersFavor.Q00297_GatekeepersFavor;
import quests.Q00298_LizardmensConspiracy.Q00298_LizardmensConspiracy;
import quests.Q00299_GatherIngredientsForPie.Q00299_GatherIngredientsForPie;
import quests.Q00300_HuntingLetoLizardman.Q00300_HuntingLetoLizardman;
import quests.Q00303_CollectArrowheads.Q00303_CollectArrowheads;
import quests.Q00306_CrystalOfFireAndIce.Q00306_CrystalOfFireAndIce;
import quests.Q00313_CollectSpores.Q00313_CollectSpores;
import quests.Q00316_DestroyPlagueCarriers.Q00316_DestroyPlagueCarriers;
import quests.Q00317_CatchTheWind.Q00317_CatchTheWind;
import quests.Q00319_ScentOfDeath.Q00319_ScentOfDeath;
import quests.Q00320_BonesTellTheFuture.Q00320_BonesTellTheFuture;
import quests.Q00324_SweetestVenom.Q00324_SweetestVenom;
import quests.Q00325_GrimCollector.Q00325_GrimCollector;
import quests.Q00326_VanquishRemnants.Q00326_VanquishRemnants;
import quests.Q00327_RecoverTheFarmland.Q00327_RecoverTheFarmland;
import quests.Q00328_SenseForBusiness.Q00328_SenseForBusiness;
import quests.Q00329_CuriosityOfADwarf.Q00329_CuriosityOfADwarf;
import quests.Q00330_AdeptOfTaste.Q00330_AdeptOfTaste;
import quests.Q00331_ArrowOfVengeance.Q00331_ArrowOfVengeance;
import quests.Q00333_HuntOfTheBlackLion.Q00333_HuntOfTheBlackLion;
import quests.Q00334_TheWishingPotion.Q00334_TheWishingPotion;
import quests.Q00335_TheSongOfTheHunter.Q00335_TheSongOfTheHunter;
import quests.Q00336_CoinsOfMagic.Q00336_CoinsOfMagic;
import quests.Q00337_AudienceWithTheLandDragon.Q00337_AudienceWithTheLandDragon;
import quests.Q00338_AlligatorHunter.Q00338_AlligatorHunter;
import quests.Q00340_SubjugationOfLizardmen.Q00340_SubjugationOfLizardmen;
import quests.Q00341_HuntingForWildBeasts.Q00341_HuntingForWildBeasts;
import quests.Q00343_UnderTheShadowOfTheIvoryTower.Q00343_UnderTheShadowOfTheIvoryTower;
import quests.Q00344_1000YearsTheEndOfLamentation.Q00344_1000YearsTheEndOfLamentation;
import quests.Q00345_MethodToRaiseTheDead.Q00345_MethodToRaiseTheDead;
import quests.Q00347_GoGetTheCalculator.Q00347_GoGetTheCalculator;
import quests.Q00348_AnArrogantSearch.Q00348_AnArrogantSearch;
import quests.Q00350_EnhanceYourWeapon.Q00350_EnhanceYourWeapon;
import quests.Q00351_BlackSwan.Q00351_BlackSwan;
import quests.Q00352_HelpRoodRaiseANewPet.Q00352_HelpRoodRaiseANewPet;
import quests.Q00353_PowerOfDarkness.Q00353_PowerOfDarkness;
import quests.Q00354_ConquestOfAlligatorIsland.Q00354_ConquestOfAlligatorIsland;
import quests.Q00355_FamilyHonor.Q00355_FamilyHonor;
import quests.Q00356_DigUpTheSeaOfSpores.Q00356_DigUpTheSeaOfSpores;
import quests.Q00357_WarehouseKeepersAmbition.Q00357_WarehouseKeepersAmbition;
import quests.Q00358_IllegitimateChildOfTheGoddess.Q00358_IllegitimateChildOfTheGoddess;
import quests.Q00359_ForASleeplessDeadman.Q00359_ForASleeplessDeadman;
import quests.Q00360_PlunderTheirSupplies.Q00360_PlunderTheirSupplies;
import quests.Q00362_BardsMandolin.Q00362_BardsMandolin;
import quests.Q00363_SorrowfulSoundOfFlute.Q00363_SorrowfulSoundOfFlute;
import quests.Q00364_JovialAccordion.Q00364_JovialAccordion;
import quests.Q00365_DevilsLegacy.Q00365_DevilsLegacy;
import quests.Q00366_SilverHairedShaman.Q00366_SilverHairedShaman;
import quests.Q00367_ElectrifyingRecharge.Q00367_ElectrifyingRecharge;
import quests.Q00368_TrespassingIntoTheHolyGround.Q00368_TrespassingIntoTheHolyGround;
import quests.Q00369_CollectorOfJewels.Q00369_CollectorOfJewels;
import quests.Q00370_AnElderSowsSeeds.Q00370_AnElderSowsSeeds;
import quests.Q00371_ShrieksOfGhosts.Q00371_ShrieksOfGhosts;
import quests.Q00372_LegacyOfInsolence.Q00372_LegacyOfInsolence;
import quests.Q00373_SupplierOfReagents.Q00373_SupplierOfReagents;
import quests.Q00374_WhisperOfDreamsPart1.Q00374_WhisperOfDreamsPart1;
import quests.Q00375_WhisperOfDreamsPart2.Q00375_WhisperOfDreamsPart2;
import quests.Q00376_ExplorationOfTheGiantsCavePart1.Q00376_ExplorationOfTheGiantsCavePart1;
import quests.Q00377_ExplorationOfTheGiantsCavePart2.Q00377_ExplorationOfTheGiantsCavePart2;
import quests.Q00378_GrandFeast.Q00378_GrandFeast;
import quests.Q00379_FantasyWine.Q00379_FantasyWine;
import quests.Q00380_BringOutTheFlavorOfIngredients.Q00380_BringOutTheFlavorOfIngredients;
import quests.Q00381_LetsBecomeARoyalMember.Q00381_LetsBecomeARoyalMember;
import quests.Q00382_KailsMagicCoin.Q00382_KailsMagicCoin;
import quests.Q00383_TreasureHunt.Q00383_TreasureHunt;
import quests.Q00384_WarehouseKeepersPastime.Q00384_WarehouseKeepersPastime;
import quests.Q00385_YokeOfThePast.Q00385_YokeOfThePast;
import quests.Q00386_StolenDignity.Q00386_StolenDignity;
import quests.Q00401_PathOfTheWarrior.Q00401_PathOfTheWarrior;
import quests.Q00402_PathOfTheHumanKnight.Q00402_PathOfTheHumanKnight;
import quests.Q00403_PathOfTheRogue.Q00403_PathOfTheRogue;
import quests.Q00404_PathOfTheHumanWizard.Q00404_PathOfTheHumanWizard;
import quests.Q00405_PathOfTheCleric.Q00405_PathOfTheCleric;
import quests.Q00406_PathOfTheElvenKnight.Q00406_PathOfTheElvenKnight;
import quests.Q00407_PathOfTheElvenScout.Q00407_PathOfTheElvenScout;
import quests.Q00408_PathOfTheElvenWizard.Q00408_PathOfTheElvenWizard;
import quests.Q00409_PathOfTheElvenOracle.Q00409_PathOfTheElvenOracle;
import quests.Q00410_PathOfThePalusKnight.Q00410_PathOfThePalusKnight;
import quests.Q00411_PathOfTheAssassin.Q00411_PathOfTheAssassin;
import quests.Q00412_PathOfTheDarkWizard.Q00412_PathOfTheDarkWizard;
import quests.Q00413_PathOfTheShillienOracle.Q00413_PathOfTheShillienOracle;
import quests.Q00414_PathOfTheOrcRaider.Q00414_PathOfTheOrcRaider;
import quests.Q00415_PathOfTheOrcMonk.Q00415_PathOfTheOrcMonk;
import quests.Q00416_PathOfTheOrcShaman.Q00416_PathOfTheOrcShaman;
import quests.Q00417_PathOfTheScavenger.Q00417_PathOfTheScavenger;
import quests.Q00418_PathOfTheArtisan.Q00418_PathOfTheArtisan;
import quests.Q00419_GetAPet.Q00419_GetAPet;
import quests.Q00420_LittleWing.Q00420_LittleWing;
import quests.Q00421_LittleWingsBigAdventure.Q00421_LittleWingsBigAdventure;
import quests.Q00422_RepentYourSins.Q00422_RepentYourSins;
import quests.Q00426_QuestForFishingShot.Q00426_QuestForFishingShot;
import quests.Q00431_WeddingMarch.Q00431_WeddingMarch;
import quests.Q00432_BirthdayPartySong.Q00432_BirthdayPartySong;
import quests.Q00501_ProofOfClanAlliance.Q00501_ProofOfClanAlliance;
import quests.Q00503_PursuitOfClanAmbition.Q00503_PursuitOfClanAmbition;
import quests.Q00504_CompetitionForTheBanditStronghold.Q00504_CompetitionForTheBanditStronghold;
import quests.Q00505_BloodOffering.Q00505_BloodOffering;
import quests.Q00508_AClansReputation.Q00508_AClansReputation;
import quests.Q00509_AClansFame.Q00509_AClansFame;
import quests.Q00510_AClansPrestige.Q00510_AClansPrestige;
import quests.Q00601_WatchingEyes.Q00601_WatchingEyes;
import quests.Q00602_ShadowOfLight.Q00602_ShadowOfLight;
import quests.Q00603_DaimonTheWhiteEyedPart1.Q00603_DaimonTheWhiteEyedPart1;
import quests.Q00604_DaimonTheWhiteEyedPart2.Q00604_DaimonTheWhiteEyedPart2;
import quests.Q00605_AllianceWithKetraOrcs.Q00605_AllianceWithKetraOrcs;
import quests.Q00606_BattleAgainstVarkaSilenos.Q00606_BattleAgainstVarkaSilenos;
import quests.Q00607_ProveYourCourageKetra.Q00607_ProveYourCourageKetra;
import quests.Q00608_SlayTheEnemyCommanderKetra.Q00608_SlayTheEnemyCommanderKetra;
import quests.Q00609_MagicalPowerOfWaterPart1.Q00609_MagicalPowerOfWaterPart1;
import quests.Q00610_MagicalPowerOfWaterPart2.Q00610_MagicalPowerOfWaterPart2;
import quests.Q00611_AllianceWithVarkaSilenos.Q00611_AllianceWithVarkaSilenos;
import quests.Q00612_BattleAgainstKetraOrcs.Q00612_BattleAgainstKetraOrcs;
import quests.Q00613_ProveYourCourageVarka.Q00613_ProveYourCourageVarka;
import quests.Q00614_SlayTheEnemyCommanderVarka.Q00614_SlayTheEnemyCommanderVarka;
import quests.Q00615_MagicalPowerOfFirePart1.Q00615_MagicalPowerOfFirePart1;
import quests.Q00616_MagicalPowerOfFirePart2.Q00616_MagicalPowerOfFirePart2;
import quests.Q00617_GatherTheFlames.Q00617_GatherTheFlames;
import quests.Q00618_IntoTheFlame.Q00618_IntoTheFlame;
import quests.Q00619_RelicsOfTheOldEmpire.Q00619_RelicsOfTheOldEmpire;
import quests.Q00620_FourGoblets.Q00620_FourGoblets;
import quests.Q00621_EggDelivery.Q00621_EggDelivery;
import quests.Q00622_SpecialtyLiquorDelivery.Q00622_SpecialtyLiquorDelivery;
import quests.Q00623_TheFinestFood.Q00623_TheFinestFood;
import quests.Q00624_TheFinestIngredientsPart1.Q00624_TheFinestIngredientsPart1;
import quests.Q00625_TheFinestIngredientsPart2.Q00625_TheFinestIngredientsPart2;
import quests.Q00626_ADarkTwilight.Q00626_ADarkTwilight;
import quests.Q00627_HeartInSearchOfPower.Q00627_HeartInSearchOfPower;
import quests.Q00628_HuntGoldenRam.Q00628_HuntGoldenRam;
import quests.Q00629_CleanUpTheSwampOfScreams.Q00629_CleanUpTheSwampOfScreams;
import quests.Q00631_DeliciousTopChoiceMeat.Q00631_DeliciousTopChoiceMeat;
import quests.Q00632_NecromancersRequest.Q00632_NecromancersRequest;
import quests.Q00633_InTheForgottenVillage.Q00633_InTheForgottenVillage;
import quests.Q00634_InSearchOfFragmentsOfDimension.Q00634_InSearchOfFragmentsOfDimension;
import quests.Q00635_IntoTheDimensionalRift.Q00635_IntoTheDimensionalRift;
import quests.Q00636_TruthBeyondTheGate.Q00636_TruthBeyondTheGate;
import quests.Q00637_ThroughOnceMore.Q00637_ThroughOnceMore;
import quests.Q00638_SeekersOfTheHolyGrail.Q00638_SeekersOfTheHolyGrail;
import quests.Q00639_GuardiansOfTheHolyGrail.Q00639_GuardiansOfTheHolyGrail;
import quests.Q00640_TheZeroHour.Q00640_TheZeroHour;
import quests.Q00641_AttackSailren.Q00641_AttackSailren;
import quests.Q00642_APowerfulPrimevalCreature.Q00642_APowerfulPrimevalCreature;
import quests.Q00643_RiseAndFallOfTheElrokiTribe.Q00643_RiseAndFallOfTheElrokiTribe;
import quests.Q00644_GraveRobberAnnihilation.Q00644_GraveRobberAnnihilation;
import quests.Q00645_GhostsOfBatur.Q00645_GhostsOfBatur;
import quests.Q00646_SignsOfRevolt.Q00646_SignsOfRevolt;
import quests.Q00647_InfluxOfMachines.Q00647_InfluxOfMachines;
import quests.Q00648_AnIceMerchantsDream.Q00648_AnIceMerchantsDream;
import quests.Q00649_ALooterAndARailroadMan.Q00649_ALooterAndARailroadMan;
import quests.Q00650_ABrokenDream.Q00650_ABrokenDream;
import quests.Q00651_RunawayYouth.Q00651_RunawayYouth;
import quests.Q00652_AnAgedExAdventurer.Q00652_AnAgedExAdventurer;
import quests.Q00653_WildMaiden.Q00653_WildMaiden;
import quests.Q00654_JourneyToASettlement.Q00654_JourneyToASettlement;
import quests.Q00655_AGrandPlanForTamingWildBeasts.Q00655_AGrandPlanForTamingWildBeasts;
import quests.Q00659_IdRatherBeCollectingFairyBreath.Q00659_IdRatherBeCollectingFairyBreath;
import quests.Q00660_AidingTheFloranVillage.Q00660_AidingTheFloranVillage;
import quests.Q00661_MakingTheHarvestGroundsSafe.Q00661_MakingTheHarvestGroundsSafe;
import quests.Q00662_AGameOfCards.Q00662_AGameOfCards;
import quests.Q00663_SeductiveWhispers.Q00663_SeductiveWhispers;
import quests.Q00688_DefeatTheElrokianRaiders.Q00688_DefeatTheElrokianRaiders;
import quests.Q00999_T0Tutorial.Q00999_T0Tutorial;

/**
 * @author NosBit, Mobius
 */
public class QuestMasterHandler
{
	private static final Logger LOGGER = Logger.getLogger(QuestMasterHandler.class.getName());
	
	private static final Class<?>[] QUESTS =
	{
		Q00001_LettersOfLove.class,
		Q00002_WhatWomenWant.class,
		Q00003_WillTheSealBeBroken.class,
		Q00004_LongLiveThePaagrioLord.class,
		Q00005_MinersFavor.class,
		Q00006_StepIntoTheFuture.class,
		Q00007_ATripBegins.class,
		Q00008_AnAdventureBegins.class,
		Q00009_IntoTheCityOfHumans.class,
		Q00010_IntoTheWorld.class,
		Q00011_SecretMeetingWithKetraOrcs.class,
		Q00012_SecretMeetingWithVarkaSilenos.class,
		Q00013_ParcelDelivery.class,
		Q00014_WhereaboutsOfTheArchaeologist.class,
		Q00015_SweetWhispers.class,
		Q00016_TheComingDarkness.class,
		Q00017_LightAndDarkness.class,
		Q00018_MeetingWithTheGoldenRam.class,
		Q00019_GoToThePastureland.class,
		Q00020_BringUpWithLove.class,
		Q00021_HiddenTruth.class,
		Q00022_TragedyInVonHellmannForest.class,
		Q00023_LidiasHeart.class,
		Q00024_InhabitantsOfTheForestOfTheDead.class,
		Q00025_HidingBehindTheTruth.class,
		Q00027_ChestCaughtWithABaitOfWind.class,
		Q00028_ChestCaughtWithABaitOfIcyAir.class,
		Q00029_ChestCaughtWithABaitOfEarth.class,
		Q00030_ChestCaughtWithABaitOfFire.class,
		Q00031_SecretBuriedInTheSwamp.class,
		Q00032_AnObviousLie.class,
		Q00033_MakeAPairOfDressShoes.class,
		Q00034_InSearchOfCloth.class,
		Q00035_FindGlitteringJewelry.class,
		Q00036_MakeASewingKit.class,
		Q00037_MakeFormalWear.class,
		Q00038_DragonFangs.class,
		Q00039_RedEyedInvaders.class,
		Q00042_HelpTheUncle.class,
		Q00043_HelpTheSister.class,
		Q00044_HelpTheSon.class,
		Q00045_ToTalkingIsland.class,
		Q00046_OnceMoreInTheArmsOfTheMotherTree.class,
		Q00047_IntoTheDarkElvenForest.class,
		Q00048_ToTheImmortalPlateau.class,
		Q00049_TheRoadHome.class,
		Q00050_LanoscosSpecialBait.class,
		Q00051_OFullesSpecialBait.class,
		Q00052_WilliesSpecialBait.class,
		Q00053_LinnaeusSpecialBait.class,
		Q00070_SagaOfThePhoenixKnight.class,
		Q00071_SagaOfEvasTemplar.class,
		Q00072_SagaOfTheSwordMuse.class,
		Q00073_SagaOfTheDuelist.class,
		Q00074_SagaOfTheDreadnought.class,
		Q00075_SagaOfTheTitan.class,
		Q00076_SagaOfTheGrandKhavatari.class,
		Q00077_SagaOfTheDominator.class,
		Q00078_SagaOfTheDoomcryer.class,
		Q00079_SagaOfTheAdventurer.class,
		Q00080_SagaOfTheWindRider.class,
		Q00081_SagaOfTheGhostHunter.class,
		Q00082_SagaOfTheSagittarius.class,
		Q00083_SagaOfTheMoonlightSentinel.class,
		Q00084_SagaOfTheGhostSentinel.class,
		Q00085_SagaOfTheCardinal.class,
		Q00086_SagaOfTheHierophant.class,
		Q00087_SagaOfEvasSaint.class,
		Q00088_SagaOfTheArchmage.class,
		Q00089_SagaOfTheMysticMuse.class,
		Q00090_SagaOfTheStormScreamer.class,
		Q00091_SagaOfTheArcanaLord.class,
		Q00092_SagaOfTheElementalMaster.class,
		Q00093_SagaOfTheSpectralMaster.class,
		Q00094_SagaOfTheSoultaker.class,
		Q00095_SagaOfTheHellKnight.class,
		Q00096_SagaOfTheSpectralDancer.class,
		Q00097_SagaOfTheShillienTemplar.class,
		Q00098_SagaOfTheShillienSaint.class,
		Q00099_SagaOfTheFortuneSeeker.class,
		Q00100_SagaOfTheMaestro.class,
		Q00101_SwordOfSolidarity.class,
		Q00102_SeaOfSporesFever.class,
		Q00103_SpiritOfCraftsman.class,
		Q00104_SpiritOfMirrors.class,
		Q00105_SkirmishWithOrcs.class,
		Q00106_ForgottenTruth.class,
		Q00107_MercilessPunishment.class,
		Q00108_JumbleTumbleDiamondFuss.class,
		Q00109_InSearchOfTheNest.class,
		Q00110_ToThePrimevalIsle.class,
		Q00111_ElrokianHuntersProof.class,
		Q00112_WalkOfFate.class,
		Q00113_StatusOfTheBeaconTower.class,
		Q00114_ResurrectionOfAnOldManager.class,
		Q00115_TheOtherSideOfTruth.class,
		Q00116_BeyondTheHillsOfWinter.class,
		Q00117_TheOceanOfDistantStars.class,
		Q00118_ToLeadAndBeLed.class,
		Q00119_LastImperialPrince.class,
		Q00120_PavelsLastResearch.class,
		Q00121_PavelTheGiant.class,
		Q00122_OminousNews.class,
		Q00123_TheLeaderAndTheFollower.class,
		Q00124_MeetingTheElroki.class,
		Q00125_TheNameOfEvil1.class,
		Q00126_TheNameOfEvil2.class,
		Q00127_KamaelAWindowToTheFuture.class,
		Q00151_CureForFever.class,
		Q00152_ShardsOfGolem.class,
		Q00153_DeliverGoods.class,
		Q00154_SacrificeToTheSea.class,
		Q00155_FindSirWindawood.class,
		Q00156_MillenniumLove.class,
		Q00157_RecoverSmuggledGoods.class,
		Q00158_SeedOfEvil.class,
		Q00159_ProtectTheWaterSource.class,
		Q00160_NerupasRequest.class,
		Q00161_FruitOfTheMotherTree.class,
		Q00162_CurseOfTheUndergroundFortress.class,
		Q00163_LegacyOfThePoet.class,
		Q00164_BloodFiend.class,
		Q00165_ShilensHunt.class,
		Q00166_MassOfDarkness.class,
		Q00167_DwarvenKinship.class,
		Q00168_DeliverSupplies.class,
		Q00169_OffspringOfNightmares.class,
		Q00170_DangerousSeduction.class,
		Q00171_ActsOfEvil.class,
		Q00211_TrialOfTheChallenger.class,
		Q00212_TrialOfDuty.class,
		Q00213_TrialOfTheSeeker.class,
		Q00214_TrialOfTheScholar.class,
		Q00215_TrialOfThePilgrim.class,
		Q00216_TrialOfTheGuildsman.class,
		Q00217_TestimonyOfTrust.class,
		Q00218_TestimonyOfLife.class,
		Q00219_TestimonyOfFate.class,
		Q00220_TestimonyOfGlory.class,
		Q00221_TestimonyOfProsperity.class,
		Q00222_TestOfTheDuelist.class,
		Q00223_TestOfTheChampion.class,
		Q00224_TestOfSagittarius.class,
		Q00225_TestOfTheSearcher.class,
		Q00226_TestOfTheHealer.class,
		Q00227_TestOfTheReformer.class,
		Q00228_TestOfMagus.class,
		Q00229_TestOfWitchcraft.class,
		Q00230_TestOfTheSummoner.class,
		Q00231_TestOfTheMaestro.class,
		Q00232_TestOfTheLord.class,
		Q00233_TestOfTheWarSpirit.class,
		Q00234_FatesWhisper.class,
		Q00235_MimirsElixir.class,
		Q00241_PossessorOfAPreciousSoul1.class,
		Q00242_PossessorOfAPreciousSoul2.class,
		Q00246_PossessorOfAPreciousSoul3.class,
		Q00247_PossessorOfAPreciousSoul4.class,
		Q00255_Tutorial.class,
		Q00257_TheGuardIsBusy.class,
		Q00258_BringWolfPelts.class,
		Q00259_RequestFromTheFarmOwner.class,
		Q00260_OrcHunting.class,
		Q00261_CollectorsDream.class,
		Q00262_TradeWithTheIvoryTower.class,
		Q00263_OrcSubjugation.class,
		Q00264_KeenClaws.class,
		Q00265_BondsOfSlavery.class,
		Q00266_PleasOfPixies.class,
		Q00267_WrathOfVerdure.class,
		Q00271_ProofOfValor.class,
		Q00272_WrathOfAncestors.class,
		Q00273_InvadersOfTheHolyLand.class,
		Q00274_SkirmishWithTheWerewolves.class,
		Q00275_DarkWingedSpies.class,
		Q00276_TotemOfTheHestui.class,
		Q00277_GatekeepersOffering.class,
		Q00291_RevengeOfTheRedbonnet.class,
		Q00292_BrigandsSweep.class,
		Q00293_TheHiddenVeins.class,
		Q00294_CovertBusiness.class,
		Q00295_DreamingOfTheSkies.class,
		Q00296_TarantulasSpiderSilk.class,
		Q00297_GatekeepersFavor.class,
		Q00298_LizardmensConspiracy.class,
		Q00299_GatherIngredientsForPie.class,
		Q00300_HuntingLetoLizardman.class,
		Q00303_CollectArrowheads.class,
		Q00306_CrystalOfFireAndIce.class,
		Q00313_CollectSpores.class,
		Q00316_DestroyPlagueCarriers.class,
		Q00317_CatchTheWind.class,
		Q00319_ScentOfDeath.class,
		Q00320_BonesTellTheFuture.class,
		Q00324_SweetestVenom.class,
		Q00325_GrimCollector.class,
		Q00326_VanquishRemnants.class,
		Q00327_RecoverTheFarmland.class,
		Q00328_SenseForBusiness.class,
		Q00329_CuriosityOfADwarf.class,
		Q00330_AdeptOfTaste.class,
		Q00331_ArrowOfVengeance.class,
		Q00333_HuntOfTheBlackLion.class,
		Q00334_TheWishingPotion.class,
		Q00335_TheSongOfTheHunter.class,
		Q00336_CoinsOfMagic.class,
		Q00337_AudienceWithTheLandDragon.class,
		Q00338_AlligatorHunter.class,
		Q00340_SubjugationOfLizardmen.class,
		Q00341_HuntingForWildBeasts.class,
		Q00343_UnderTheShadowOfTheIvoryTower.class,
		Q00344_1000YearsTheEndOfLamentation.class,
		Q00345_MethodToRaiseTheDead.class,
		Q00347_GoGetTheCalculator.class,
		Q00348_AnArrogantSearch.class,
		Q00350_EnhanceYourWeapon.class,
		Q00351_BlackSwan.class,
		Q00352_HelpRoodRaiseANewPet.class,
		Q00353_PowerOfDarkness.class,
		Q00354_ConquestOfAlligatorIsland.class,
		Q00355_FamilyHonor.class,
		Q00356_DigUpTheSeaOfSpores.class,
		Q00357_WarehouseKeepersAmbition.class,
		Q00358_IllegitimateChildOfTheGoddess.class,
		Q00359_ForASleeplessDeadman.class,
		Q00360_PlunderTheirSupplies.class,
		Q00362_BardsMandolin.class,
		Q00363_SorrowfulSoundOfFlute.class,
		Q00364_JovialAccordion.class,
		Q00365_DevilsLegacy.class,
		Q00366_SilverHairedShaman.class,
		Q00367_ElectrifyingRecharge.class,
		Q00368_TrespassingIntoTheHolyGround.class,
		Q00369_CollectorOfJewels.class,
		Q00370_AnElderSowsSeeds.class,
		Q00371_ShrieksOfGhosts.class,
		Q00372_LegacyOfInsolence.class,
		Q00373_SupplierOfReagents.class,
		Q00374_WhisperOfDreamsPart1.class,
		Q00375_WhisperOfDreamsPart2.class,
		Q00376_ExplorationOfTheGiantsCavePart1.class,
		Q00377_ExplorationOfTheGiantsCavePart2.class,
		Q00378_GrandFeast.class,
		Q00379_FantasyWine.class,
		Q00380_BringOutTheFlavorOfIngredients.class,
		Q00381_LetsBecomeARoyalMember.class,
		Q00382_KailsMagicCoin.class,
		Q00383_TreasureHunt.class,
		Q00384_WarehouseKeepersPastime.class,
		Q00385_YokeOfThePast.class,
		Q00386_StolenDignity.class,
		Q00401_PathOfTheWarrior.class,
		Q00402_PathOfTheHumanKnight.class,
		Q00403_PathOfTheRogue.class,
		Q00404_PathOfTheHumanWizard.class,
		Q00405_PathOfTheCleric.class,
		Q00406_PathOfTheElvenKnight.class,
		Q00407_PathOfTheElvenScout.class,
		Q00408_PathOfTheElvenWizard.class,
		Q00409_PathOfTheElvenOracle.class,
		Q00410_PathOfThePalusKnight.class,
		Q00411_PathOfTheAssassin.class,
		Q00412_PathOfTheDarkWizard.class,
		Q00413_PathOfTheShillienOracle.class,
		Q00414_PathOfTheOrcRaider.class,
		Q00415_PathOfTheOrcMonk.class,
		Q00416_PathOfTheOrcShaman.class,
		Q00417_PathOfTheScavenger.class,
		Q00418_PathOfTheArtisan.class,
		Q00419_GetAPet.class,
		Q00420_LittleWing.class,
		Q00421_LittleWingsBigAdventure.class,
		Q00422_RepentYourSins.class,
		Q00426_QuestForFishingShot.class,
		Q00431_WeddingMarch.class,
		Q00432_BirthdayPartySong.class,
		Q00501_ProofOfClanAlliance.class,
		Q00503_PursuitOfClanAmbition.class,
		Q00504_CompetitionForTheBanditStronghold.class,
		Q00505_BloodOffering.class,
		Q00508_AClansReputation.class,
		Q00509_AClansFame.class,
		Q00510_AClansPrestige.class,
		Q00601_WatchingEyes.class,
		Q00602_ShadowOfLight.class,
		Q00603_DaimonTheWhiteEyedPart1.class,
		Q00604_DaimonTheWhiteEyedPart2.class,
		Q00605_AllianceWithKetraOrcs.class,
		Q00606_BattleAgainstVarkaSilenos.class,
		Q00607_ProveYourCourageKetra.class,
		Q00608_SlayTheEnemyCommanderKetra.class,
		Q00609_MagicalPowerOfWaterPart1.class,
		Q00610_MagicalPowerOfWaterPart2.class,
		Q00611_AllianceWithVarkaSilenos.class,
		Q00612_BattleAgainstKetraOrcs.class,
		Q00613_ProveYourCourageVarka.class,
		Q00614_SlayTheEnemyCommanderVarka.class,
		Q00615_MagicalPowerOfFirePart1.class,
		Q00616_MagicalPowerOfFirePart2.class,
		Q00617_GatherTheFlames.class,
		Q00618_IntoTheFlame.class,
		Q00619_RelicsOfTheOldEmpire.class,
		Q00620_FourGoblets.class,
		Q00621_EggDelivery.class,
		Q00622_SpecialtyLiquorDelivery.class,
		Q00623_TheFinestFood.class,
		Q00624_TheFinestIngredientsPart1.class,
		Q00625_TheFinestIngredientsPart2.class,
		Q00626_ADarkTwilight.class,
		Q00627_HeartInSearchOfPower.class,
		Q00628_HuntGoldenRam.class,
		Q00629_CleanUpTheSwampOfScreams.class,
		Q00631_DeliciousTopChoiceMeat.class,
		Q00632_NecromancersRequest.class,
		Q00633_InTheForgottenVillage.class,
		Q00634_InSearchOfFragmentsOfDimension.class,
		Q00635_IntoTheDimensionalRift.class,
		Q00636_TruthBeyondTheGate.class,
		Q00637_ThroughOnceMore.class,
		Q00638_SeekersOfTheHolyGrail.class,
		Q00639_GuardiansOfTheHolyGrail.class,
		Q00640_TheZeroHour.class,
		Q00641_AttackSailren.class,
		Q00642_APowerfulPrimevalCreature.class,
		Q00643_RiseAndFallOfTheElrokiTribe.class,
		Q00644_GraveRobberAnnihilation.class,
		Q00645_GhostsOfBatur.class,
		Q00646_SignsOfRevolt.class,
		Q00647_InfluxOfMachines.class,
		Q00648_AnIceMerchantsDream.class,
		Q00649_ALooterAndARailroadMan.class,
		Q00650_ABrokenDream.class,
		Q00651_RunawayYouth.class,
		Q00652_AnAgedExAdventurer.class,
		Q00653_WildMaiden.class,
		Q00654_JourneyToASettlement.class,
		Q00655_AGrandPlanForTamingWildBeasts.class,
		Q00659_IdRatherBeCollectingFairyBreath.class,
		Q00660_AidingTheFloranVillage.class,
		Q00661_MakingTheHarvestGroundsSafe.class,
		Q00662_AGameOfCards.class,
		Q00663_SeductiveWhispers.class,
		Q00688_DefeatTheElrokianRaiders.class,
		Q00999_T0Tutorial.class
	};
	
	public static void main(String[] args)
	{
		for (Class<?> quest : QUESTS)
		{
			try
			{
				quest.getDeclaredConstructor().newInstance();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, QuestMasterHandler.class.getSimpleName() + ": Failed loading " + quest.getSimpleName() + ":", e);
			}
		}
	}
}
