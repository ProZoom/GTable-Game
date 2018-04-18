package cc.fish91.gtable.engine

import cc.fish91.gtable.*
import cc.fish91.gtable.localdata.PersonRecord
import cc.fish91.gtable.plugin.Math
import cc.fish91.gtable.plugin.max
import cc.fish91.gtable.resource.StaticData

object FightScene {
    fun fight(m: MonsterData, p: FightSceneFinalData, forceEnd: Boolean = false, vararg effects: IFightEffect): Boolean {
        while (forceEnd) {
            effects.map { it.onFight(p, m) }
            m.HP -= (Math.forceMinus(p.atk + p.buff.tAtk, m.def) * (if (Math.mil_percent(p.critical)) (p.criticalDmg / 100.0) else 1.0)).toInt()
            if (!Math.mil_percent(p.miss.max(800)))
                p.HP -= Math.forceMinus(m.atk, p.def + p.buff.tDef)
            effects.map { it.onFightEnd(p, m) }
            if (m.HP <= 0 || p.HP <= 0)
                return true
        }
        return false
    }

    fun buff(b: Buff, fb: FloorBuff) {
        when (b.ability) {
            BuffAbility.ATK -> fb.tAtk += b.value
            BuffAbility.DEF -> fb.tDef += b.value
        }
    }

    fun gift(g: Gift, p: FightSceneFinalData) {
        when (g.giftType) {
            Gifts.HP_RESTORE -> p.HP += g.value
            Gifts.ATK_UP -> p.atk += g.value
            Gifts.DEF_UP -> p.def += g.value
        }
    }

    fun award(p: PersonData, m: MonsterData, isK: Boolean): Boolean {
        p.exp += m.exp
        p.gold += m.gold
        if (p.exp >= StaticData.getLimitExp(p.level)) {
            PersonRecord.personDataLevelUP()
            p.exp = 0
            p.level++
            StaticData.getLvGrow(p.roleType).let {
                p.atk += it.atk
                p.def += it.def
            }
            PersonRecord.getPersonHPLine().let {
                if (it > p.HP)
                    p.HP = it
            }
            return true
        }
        return false
    }

    fun failed(mPerson: PersonData, mFloor: Int) {
        PersonRecord.getPersonData().let {
            it.gold = mPerson.gold
            it.exp = mPerson.exp
            it.maxFloor.run { if (this < mFloor) it.maxFloor = mFloor }
            PersonRecord.storePersonData(it)
        }
    }

    fun takeDrop(floor: Int, isK: Boolean, monster: MonsterData) = takeDropId(monster, isK).run {
        if (this > 0)
            EquipEngine.create(this, if (isK) 2 else 1, floor)
        else null
    }

    private fun takeDropId(monster: MonsterData, isK: Boolean) = StaticData.getBaseMonster(monster.mId).drop.run {
        if (Math.denominatorOf(first, if (isK) 2 else 1)) second else 0
    }

}