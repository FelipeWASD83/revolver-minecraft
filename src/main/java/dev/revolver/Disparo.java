package dev.revolver;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/** O tiro das duas armas, no servidor. */
public final class Disparo {
    public static final double ALCANCE = 64.0;

    private Disparo() {}

    /** O tiro: um raio do olho até 64 blocos; acerta a primeira criatura ou o primeiro bloco no caminho. */
    public static void atirar(ServerLevel level, Player jogador, boolean outraMao, float dano) {
        atirar(level, jogador, outraMao, dano, 1, 0f);
    }

    /**
     * Um tiro com {@code bagos} balas (a escopeta solta várias), cada uma desviada até {@code espalhamento} graus da
     * mira. O dano das que acertam a mesma criatura é somado e entra de uma vez, e a invencibilidade de meio segundo
     * que o jogo dá depois de um golpe é zerada antes: sem isso a rajada e os bagos se perderiam.
     */
    public static void atirar(ServerLevel level, Player jogador, boolean outraMao, float dano, int bagos,
                              float espalhamento) {
        Vec3 olho = jogador.getEyePosition();
        Vec3 mira = jogador.getLookAngle();
        Map<Entity, Float> danos = new LinkedHashMap<>();
        for (int b = 0; b < bagos; b++) {
            Vec3 dir = desviar(mira, espalhamento, level.random);
            Vec3 fim = olho.add(dir.scale(ALCANCE));
            BlockHitResult bloco = level.clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, jogador));
            Vec3 fimReal = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
            AABB caixa = jogador.getBoundingBox().expandTowards(fimReal.subtract(olho)).inflate(1.0);
            EntityHitResult alvo = ProjectileUtil.getEntityHitResult(level, jogador, olho, fimReal, caixa,
                    e -> e != jogador && e.isPickable() && !e.isSpectator());
            if (alvo != null) {
                danos.merge(alvo.getEntity(), dano, Float::sum);
                Vec3 p = alvo.getLocation();
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, bagos > 1 ? 3 : 10, 0.1, 0.1, 0.1, 0.2);
            } else if (bloco.getType() == HitResult.Type.BLOCK) {
                BlockState estadoBloco = level.getBlockState(bloco.getBlockPos());
                Vec3 p = bloco.getLocation();
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, estadoBloco), p.x, p.y, p.z,
                        bagos > 1 ? 4 : 12, 0.05, 0.05, 0.05, 0.1);
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, bagos > 1 ? 2 : 6, 0.02, 0.02, 0.02, 0.25);
                level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, bagos > 1 ? 1 : 3, 0.02, 0.02, 0.02, 0.01);
            }
        }

        // fumaça na boca do cano (um pouco à direita e abaixo do olho, onde a mão segura)
        Vec3 direita = mira.cross(new Vec3(0, 1, 0)).normalize();
        if (outraMao) direita = direita.scale(-1);
        Vec3 boca = olho.add(mira.scale(0.9)).add(direita.scale(0.28)).add(0, -0.18, 0);
        level.sendParticles(ParticleTypes.SMOKE, boca.x, boca.y, boca.z, 8, 0.03, 0.03, 0.03, 0.02);
        level.sendParticles(ParticleTypes.POOF, boca.x, boca.y, boca.z, 2, 0.02, 0.02, 0.02, 0.01);

        for (Map.Entry<Entity, Float> e : danos.entrySet()) {
            Entity atingido = e.getKey();
            atingido.invulnerableTime = 0;
            atingido.hurt(level.damageSources().playerAttack(jogador), e.getValue());
            if (atingido instanceof LivingEntity vivo) vivo.knockback(0.3, -mira.x, -mira.z);
        }
    }

    /** Direção da mira desviada ao acaso dentro de um cone de {@code graus}. */
    static Vec3 desviar(Vec3 mira, float graus, RandomSource sorte) {
        if (graus <= 0) return mira;
        double r = Math.toRadians(graus);
        Vec3 lado = mira.cross(new Vec3(0, 1, 0));
        lado = lado.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : lado.normalize();
        Vec3 cima = lado.cross(mira).normalize();
        double a = sorte.nextDouble() * Math.PI * 2;
        double d = Math.tan(r) * Math.sqrt(sorte.nextDouble());
        return mira.add(lado.scale(Math.cos(a) * d)).add(cima.scale(Math.sin(a) * d)).normalize();
    }
}
