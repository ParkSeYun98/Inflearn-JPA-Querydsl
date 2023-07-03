package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;

    private final JPAQueryFactory jpaQueryFactory;

    public MemberJpaRepository(EntityManager em) {

        this.em = em;
        this.jpaQueryFactory = new JPAQueryFactory(em);
    }

    // 취향 차이. 위와 장단점이 있음.
//    public MemberJpaRepository(EntityManager em, JPAQueryFactory jpaQueryFactory) {
//        this.em = em;
//        this.jpaQueryFactory = jpaQueryFactory;
//    }

    public void save(Member member) {

        em.persist(member);
    }

    public Optional<Member> findById(Long id) {

        Member findMember = em.find(Member.class, id);

        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {

        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl() {

        return jpaQueryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {

        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_Querydsl(String username) {

        return jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    // builder를 통해 동적 쿼리 생성 및 성능 최적화
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();

        // StringUtils.hasText static import
        if(hasText(condition.getUsername()))
            builder.and(member.username.eq(condition.getUsername()));

        if(hasText(condition.getTeamName()))
            builder.and(team.name.eq(condition.getTeamName()));

        if(condition.getAgeGoe() != null)
            builder.and(member.age.goe(condition.getAgeGoe()));

        if(condition.getAgeLoe() != null)
            builder.and(member.age.loe(condition.getAgeLoe()));

        return jpaQueryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {

        return jpaQueryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {

        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {

        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {

        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {

        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
